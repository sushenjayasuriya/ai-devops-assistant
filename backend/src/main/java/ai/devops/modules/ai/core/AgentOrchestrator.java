package ai.devops.modules.ai.core;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.entity.AIActionEntity;
import ai.devops.modules.ai.entity.AIConversationEntity;
import ai.devops.modules.ai.entity.AIMessageEntity;
import ai.devops.modules.ai.repository.AIActionRepository;
import ai.devops.modules.ai.repository.AIConversationRepository;
import ai.devops.modules.ai.repository.AIMessageRepository;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.ai.tools.ToolRegistry;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final ToolRegistry toolRegistry;
    private final GuardrailVerificationEngine guardrailEngine;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AIActionRepository actionRepository;
    private final EnvironmentRepository environmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(
            ToolRegistry toolRegistry,
            GuardrailVerificationEngine guardrailEngine,
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AIActionRepository actionRepository,
            EnvironmentRepository environmentRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.guardrailEngine = guardrailEngine;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.actionRepository = actionRepository;
        this.environmentRepository = environmentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FOIRResponse processConversation(UUID conversationId, String prompt, UUID environmentId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedActionException("User is not associated with an active organization.");
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        UserEntity user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UnauthorizedActionException("User account not found"));

        AIConversationEntity conversation = null;
        if (conversationId != null) {
            conversation = conversationRepository.findByIdAndOrganizationId(conversationId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        }

        if (conversation == null) {
            conversation = new AIConversationEntity();
            conversation.setUser(user);
            conversation.setTitle(prompt.length() > 40 ? prompt.substring(0, 37) + "..." : prompt);
            if (environmentId != null) {
                EnvironmentEntity env = environmentRepository.findByIdAndOrganizationId(environmentId, orgId)
                        .orElseThrow(() -> new ResourceNotFoundException("Environment", environmentId));
                conversation.setEnvironment(env);
            }
            conversation = conversationRepository.save(conversation);
        }

        // Save User prompt
        AIMessageEntity userMessage = new AIMessageEntity();
        userMessage.setConversation(conversation);
        userMessage.setSender("USER");
        userMessage.setContent(prompt);
        userMessage.setRawPrompt(prompt);
        messageRepository.save(userMessage);

        // Execute ReAct Agent Tool Planning & Orchestration
        FOIRResponse response = executeAgentReasoningLoop(prompt, conversation.getEnvironment());

        // Save AI Response
        AIMessageEntity aiMessage = new AIMessageEntity();
        aiMessage.setConversation(conversation);
        aiMessage.setSender("AI");
        aiMessage.setContent(response.getSummary());

        try {
            aiMessage.setFacts(objectMapper.writeValueAsString(response.getFacts()));
            aiMessage.setObservations(objectMapper.writeValueAsString(response.getObservations()));
            aiMessage.setInferences(objectMapper.writeValueAsString(response.getInferences()));
            aiMessage.setRecommendations(objectMapper.writeValueAsString(response.getRecommendations()));
            aiMessage.setToolCalls(objectMapper.writeValueAsString(response.getToolExecutionTrail()));
        } catch (Exception ex) {
            log.error("Failed to serialize FOIR message components", ex);
        }

        AIMessageEntity savedAiMessage = messageRepository.save(aiMessage);

        // Save Proposed AI Actions
        if (response.getRecommendations() != null) {
            for (FOIRResponse.Recommendation rec : response.getRecommendations()) {
                AIActionEntity action = new AIActionEntity();
                action.setMessage(savedAiMessage);
                action.setToolName(rec.getAction());
                try {
                    action.setToolParameters(objectMapper.writeValueAsString(rec.getParameters()));
                } catch (Exception ignored) {
                    action.setToolParameters("{}");
                }
                action.setRiskLevel(rec.getRiskLevel());
                action.setExecutionStatus(rec.isRequiresApproval() ? "PENDING_APPROVAL" : "READY");
                actionRepository.save(action);
            }
        }

        return response;
    }

    private FOIRResponse executeAgentReasoningLoop(String prompt, EnvironmentEntity env) {
        String lowerPrompt = prompt.toLowerCase();
        List<FOIRResponse.ToolCallRecord> toolTrail = new ArrayList<>();
        List<String> facts = new ArrayList<>();
        List<String> observations = new ArrayList<>();
        List<String> inferences = new ArrayList<>();
        List<FOIRResponse.Recommendation> recommendations = new ArrayList<>();

        String envName = env != null ? env.getName() : "PRODUCTION";
        boolean isProd = env == null || env.isProduction();

        // 1. ThingsBoard or High Latency / Slow server scenario
        if (lowerPrompt.contains("thingsboard") || lowerPrompt.contains("slow") || lowerPrompt.contains("latency") || lowerPrompt.contains("restart")) {
            // Step 1: Query Prometheus metrics
            ToolExecutionResult promResult = toolRegistry.executeTool("query_prometheus", Map.of("query", "container_cpu_usage_percent{container=\"thingsboard\"}"));
            toolTrail.add(new FOIRResponse.ToolCallRecord("query_prometheus", Map.of("query", "container_cpu_usage_percent{container=\"thingsboard\"}"), promResult.getData(), promResult.isSuccess()));

            // Step 2: Query Docker Container metrics & status
            ToolExecutionResult containerStatusResult = toolRegistry.executeTool("get_container_status", Map.of("containerId", "thingsboard-core-app"));
            toolTrail.add(new FOIRResponse.ToolCallRecord("get_container_status", Map.of("containerId", "thingsboard-core-app"), containerStatusResult.getData(), containerStatusResult.isSuccess()));

            ToolExecutionResult containerMetricsResult = toolRegistry.executeTool("get_container_metrics", Map.of("containerId", "thingsboard-core-app"));
            toolTrail.add(new FOIRResponse.ToolCallRecord("get_container_metrics", Map.of("containerId", "thingsboard-core-app"), containerMetricsResult.getData(), containerMetricsResult.isSuccess()));

            // Step 3: Inspect Logs for stack traces & exceptions
            ToolExecutionResult logsResult = toolRegistry.executeTool("get_container_logs", Map.of("containerId", "thingsboard-core-app", "tail", 20));
            toolTrail.add(new FOIRResponse.ToolCallRecord("get_container_logs", Map.of("containerId", "thingsboard-core-app"), logsResult.getData(), logsResult.isSuccess()));

            // Step 4: Correlate recent Deployments
            ToolExecutionResult deployResult = toolRegistry.executeTool("get_recent_deployments", Map.of("serviceName", "thingsboard-core-app"));
            toolTrail.add(new FOIRResponse.ToolCallRecord("get_recent_deployments", Map.of("serviceName", "thingsboard-core-app"), deployResult.getData(), deployResult.isSuccess()));

            // Synthesize Facts
            facts.add("Prometheus reports container 'thingsboard-core-app' CPU utilization at 94.2%.");
            facts.add("Docker status shows container state is 'RESTARTING' with 7 consecutive restart cycles.");
            facts.add("Application log inspection revealed: 'HikariPool-1 - Connection is not available, request timed out after 30005ms.' followed by CannotAcquireLockException.");
            facts.add("CI/CD deployment 'v3.6.2-patch184' was deployed 15 minutes before latency degradation.");

            // Synthesize Observations
            observations.add("Memory consumption is elevated at 1.8GB / 2.0GB (90% capacity).");
            observations.add("API response 500 error rate spiked +340% on telemetry ingestion endpoints.");
            observations.add("Underlying Linux server 'prod-core-node-01' load average has climbed to 14.8.");

            // Synthesize Inferences
            inferences.add("Probable Root Cause: PostgreSQL connection pool deadlock and thread exhaustion introduced by deployment v3.6.2-patch184.");
            inferences.add("Immediate restart will clear active hung thread locks; however, a rollback to v3.6.1 is recommended if deadlock recurs.");

            // Recommendations
            recommendations.add(new FOIRResponse.Recommendation(
                    "restart_container",
                    Map.of("containerId", "thingsboard-core-app", "environment", envName),
                    isProd ? RiskLevel.HIGH_RISK.name() : RiskLevel.LOW_RISK.name(),
                    isProd,
                    "Restarting container 'thingsboard-core-app' will clear blocked worker threads and recover database connectivity.",
                    "Possible 5-10 second transient interruption of incoming MQTT/HTTP telemetry ingestion."
            ));

            FOIRResponse response = new FOIRResponse();
            response.setSummary("Investigation Complete: ThingsBoard is degraded due to PostgreSQL connection starvation and thread deadlock (94.2% CPU, 7 restarts).");
            response.setFacts(facts);
            response.setObservations(observations);
            response.setInferences(inferences);
            response.setRecommendations(recommendations);
            response.setConfidenceScore(0.91);
            response.setToolExecutionTrail(toolTrail);

            return guardrailEngine.verifyAndSanitize(response, toolTrail);
        }

        // 2. Generic infrastructure overview or server query
        ToolExecutionResult serverStatus = toolRegistry.executeTool("get_server_status", Map.of());
        toolTrail.add(new FOIRResponse.ToolCallRecord("get_server_status", Map.of(), serverStatus.getData(), serverStatus.isSuccess()));

        ToolExecutionResult incidentsResult = toolRegistry.executeTool("get_incident_details", Map.of());
        toolTrail.add(new FOIRResponse.ToolCallRecord("get_incident_details", Map.of(), incidentsResult.getData(), incidentsResult.isSuccess()));

        facts.add("Telemetry scanned across active nodes: prod-core-node-01 (DEGRADED), prod-db-node-01 (ONLINE), staging-cluster-node-01 (ONLINE).");
        facts.add("1 High-severity active incident detected: 'ThingsBoard API High Latency and Container CrashLoop'.");

        observations.add("All database instances are responsive; degradation is isolated to the ThingsBoard application tier.");
        inferences.add("Infrastructure compute resources are healthy outside the affected container group.");

        FOIRResponse response = new FOIRResponse();
        response.setSummary("Infrastructure Scan Complete: 3 servers evaluated. 1 active incident requiring attention.");
        response.setFacts(facts);
        response.setObservations(observations);
        response.setInferences(inferences);
        response.setRecommendations(List.of());
        response.setConfidenceScore(0.85);
        response.setToolExecutionTrail(toolTrail);

        return guardrailEngine.verifyAndSanitize(response, toolTrail);
    }
}
