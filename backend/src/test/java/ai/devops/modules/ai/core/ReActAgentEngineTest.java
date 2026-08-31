package ai.devops.modules.ai.core;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.entity.AIConversationEntity;
import ai.devops.modules.ai.entity.AIMessageEntity;
import ai.devops.modules.ai.llm.*;
import ai.devops.modules.ai.repository.AIActionRepository;
import ai.devops.modules.ai.repository.AIConversationRepository;
import ai.devops.modules.ai.repository.AIMessageRepository;
import ai.devops.modules.ai.security.PromptSanitizer;
import ai.devops.modules.ai.security.SecretMasker;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.ai.tools.ToolRegistry;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReActAgentEngineTest {

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private LlmClientFactory llmClientFactory;

    @Mock
    private LlmClient mockLlmClient;

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private AIMessageRepository messageRepository;

    @Mock
    private AIActionRepository actionRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UserRepository userRepository;

    private AgentOrchestrator orchestrator;
    private PromptSanitizer promptSanitizer;
    private SecretMasker secretMasker;
    private GuardrailVerificationEngine guardrailEngine;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        promptSanitizer = new PromptSanitizer();
        secretMasker = new SecretMasker();
        guardrailEngine = new GuardrailVerificationEngine();
        objectMapper = new ObjectMapper();

        orchestrator = new AgentOrchestrator(
                toolRegistry,
                llmClientFactory,
                promptSanitizer,
                secretMasker,
                guardrailEngine,
                conversationRepository,
                messageRepository,
                actionRepository,
                environmentRepository,
                userRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("ReAct Agent executes multi-turn tool calling and synthesizes FOIR response")
    void testReActAgentReasoningLoop() {
        UUID orgId = UUID.randomUUID();
        UUID envId = UUID.randomUUID();
        String userEmail = "admin@devops.ai";

        UserEntity user = new UserEntity();
        user.setEmail(userEmail);
        OrganizationEntity org = new OrganizationEntity("Acme Corp", "acme-corp");
        org.setId(orgId);
        user.setOrganization(org);

        EnvironmentEntity env = new EnvironmentEntity(org, "PRODUCTION", "Prod", true);
        env.setId(envId);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(llmClientFactory.getClient()).thenReturn(mockLlmClient);
        when(mockLlmClient.getProviderName()).thenReturn("mock");

        DevOpsTool mockTool = new DevOpsTool() {
            @Override
            public String getName() { return "query_prometheus"; }
            @Override
            public String getDescription() { return "Query Prometheus"; }
            @Override
            public Map<String, String> getParameterSchema() { return Map.of("query", "PromQL query string"); }
            @Override
            public Set<String> getRequiredParameters() { return Set.of("query"); }
            @Override
            public Set<String> getAllowedParameters() { return Set.of("query"); }
            @Override
            public RiskLevel getRiskLevel() { return RiskLevel.LOW_RISK; }
            @Override
            public Role getRequiredRole() { return Role.VIEWER; }
            @Override
            public boolean isReadOnly() { return true; }
            @Override
            public boolean requiresProductionApproval() { return false; }
            @Override
            public ToolExecutionResult execute(Map<String, Object> parameters) {
                return ToolExecutionResult.ok("query_prometheus", Map.of("value", "94.2"));
            }
        };

        when(toolRegistry.getAllTools()).thenReturn(List.of(mockTool));
        when(toolRegistry.executeTool(eq("query_prometheus"), any())).thenReturn(ToolExecutionResult.ok("query_prometheus", Map.of("value", "94.2")));

        // Step 1: LLM outputs tool call
        LlmToolCall toolCall = new LlmToolCall("call_1", "query_prometheus", Map.of("query", "container_cpu_usage_percent"));
        LlmResponse turn1 = LlmResponse.withToolCalls(List.of(toolCall), "Inspecting Prometheus telemetry");

        // Step 2: LLM outputs final FOIR synthesis
        String finalFoir = """
        FACTS:
        - CPU is at 94.2% based on live Prometheus scrape.
        
        OBSERVATIONS:
        - Workload is saturating CPU threshold.
        
        INFERENCES:
        - Thread deadlock following latest release.
        
        RECOMMENDATIONS:
        - Restart the container to recover.
        """;
        LlmResponse turn2 = LlmResponse.withContent(finalFoir);

        when(mockLlmClient.generateChat(anyList(), anyList()))
                .thenReturn(turn1)
                .thenReturn(turn2);

        try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentOrganizationId).thenReturn(orgId);
            securityUtilsMock.when(SecurityUtils::getCurrentUserEmail).thenReturn(userEmail);

            FOIRResponse response = orchestrator.processConversation(null, "Investigate ThingsBoard slowness", null);

            assertNotNull(response);
            assertEquals(1, response.getToolExecutionTrail().size());
            assertEquals("query_prometheus", response.getToolExecutionTrail().get(0).getToolName());
            assertTrue(response.getToolExecutionTrail().get(0).isSuccess());
            assertFalse(response.getFacts().isEmpty());
            assertFalse(response.getRecommendations().isEmpty());
            assertEquals(0.91, response.getConfidenceScore(), 0.001);
        }
    }
}
