package ai.devops.modules.ai.llm.provider;

import ai.devops.modules.ai.llm.LlmClient;
import ai.devops.modules.ai.llm.LlmMessage;
import ai.devops.modules.ai.llm.LlmResponse;
import ai.devops.modules.ai.llm.LlmToolCall;
import ai.devops.modules.ai.tools.DevOpsTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MockLlmClient implements LlmClient {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public LlmResponse generateChat(List<LlmMessage> messages, List<DevOpsTool> availableTools) {
        // Inspect the conversation trajectory
        LlmMessage lastMsg = messages.get(messages.size() - 1);
        String lastContent = lastMsg.getContent() != null ? lastMsg.getContent().toLowerCase() : "";

        // If the last message was a tool result, synthesize the final FOIR diagnosis!
        if (lastMsg.getRole() == LlmMessage.Role.TOOL) {
            String synthesizedFoir = """
            FACTS:
            - Target workload responded with status telemetry and metric threshold measurements.
            - Monitored instance logs indicate resource pressure following the recent deployment.
            
            OBSERVATIONS:
            - Telemetry ingestion queues are operating near max saturation capacity.
            - Memory footprint and container restart counts match an unexpected deadlock scenario.
            
            INFERENCES:
            - High connection pool acquisition latency caused request timeout cascades.
            - Rollback or approved service restart is the safest path to restore health.
            
            RECOMMENDATIONS:
            - Restart the affected container or deployment to reset the deadlocked connection pool.
            - Rollback the recent patch release if latency does not normalize within 5 minutes.
            """;
            return LlmResponse.withContent(synthesizedFoir);
        }

        // If user prompt mentions container/thingsboard/latency/incident, call telemetry tools first!
        if (lastContent.contains("thingsboard") || lastContent.contains("container") || lastContent.contains("incident") || lastContent.contains("latency")) {
            List<LlmToolCall> toolCalls = new ArrayList<>();
            Map<String, Object> args = new HashMap<>();
            args.put("query", "container_cpu_usage_percent{container=\"thingsboard-core-app\"}");
            toolCalls.add(new LlmToolCall(UUID.randomUUID().toString(), "query_prometheus", args));
            return LlmResponse.withToolCalls(toolCalls, "I will inspect real-time Prometheus CPU telemetry and container logs to diagnose the anomaly.");
        }

        if (lastContent.contains("kubernetes") || lastContent.contains("pod") || lastContent.contains("cluster")) {
            List<LlmToolCall> toolCalls = new ArrayList<>();
            Map<String, Object> args = new HashMap<>();
            args.put("namespace", "default");
            toolCalls.add(new LlmToolCall(UUID.randomUUID().toString(), "get_kubernetes_pods", args));
            return LlmResponse.withToolCalls(toolCalls, "I am querying the Kubernetes cluster to inspect live pod lifecycle states and restarts.");
        }

        if (lastContent.contains("server") || lastContent.contains("host") || lastContent.contains("ssh")) {
            List<LlmToolCall> toolCalls = new ArrayList<>();
            Map<String, Object> args = new HashMap<>();
            args.put("host", "10.0.10.15");
            toolCalls.add(new LlmToolCall(UUID.randomUUID().toString(), "get_server_status", args));
            return LlmResponse.withToolCalls(toolCalls, "Querying live host telemetry and load averages via SSH client.");
        }

        // Generic response
        return LlmResponse.withContent("""
        FACTS:
        - Infrastructure components and monitoring telemetry are active.
        
        OBSERVATIONS:
        - No active critical anomalies detected for the specified query.
        
        INFERENCES:
        - Current system baseline is nominal.
        
        RECOMMENDATIONS:
        - Continue monitoring cluster health and Prometheus alerts.
        """);
    }
}
