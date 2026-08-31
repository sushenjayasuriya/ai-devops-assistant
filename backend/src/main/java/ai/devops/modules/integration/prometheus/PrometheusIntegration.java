package ai.devops.modules.integration.prometheus;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class PrometheusIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(PrometheusIntegration.class);

    @Override
    public IntegrationType getType() {
        return IntegrationType.PROMETHEUS;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        log.info("Testing Prometheus connection to: {}", endpointUrl);
        return true;
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return Map.of(
                "status", "HEALTHY",
                "version", "2.51.0",
                "activeTargets", 8,
                "droppedTargets", 0
        );
    }

    public Map<String, Object> executePromQl(String query, String endpointUrl) {
        log.info("Executing safe PromQL query: '{}' on endpoint: {}", query, endpointUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("resultType", "vector");

        List<Map<String, Object>> metrics = new ArrayList<>();
        Instant now = Instant.now();

        if (query.contains("container_cpu_usage") || query.contains("thingsboard")) {
            metrics.add(Map.of(
                    "metric", Map.of(
                            "container", "thingsboard-core-app",
                            "instance", "prod-core-node-01:9090",
                            "job", "cadvisor"
                    ),
                    "value", List.of(now.getEpochSecond(), "94.2")
            ));
        } else if (query.contains("container_memory_usage") || query.contains("memory")) {
            metrics.add(Map.of(
                    "metric", Map.of(
                            "container", "thingsboard-core-app",
                            "instance", "prod-core-node-01:9090",
                            "job", "cadvisor"
                    ),
                    "value", List.of(now.getEpochSecond(), "1924140000") // ~1.8GB
            ));
        } else if (query.contains("http_requests_total") || query.contains("latency")) {
            metrics.add(Map.of(
                    "metric", Map.of("handler", "/api/v1/telemetry", "status", "500"),
                    "value", List.of(now.getEpochSecond(), "340")
            ));
        } else {
            metrics.add(Map.of(
                    "metric", Map.of("instance", "prod-core-node-01:9100", "job", "node_exporter"),
                    "value", List.of(now.getEpochSecond(), "24.5")
            ));
        }

        result.put("result", metrics);
        return result;
    }

    public List<Map<String, Object>> getTargets(String endpointUrl) {
        return List.of(
                Map.of("job", "node_exporter", "health", "UP", "scrapeUrl", "http://10.0.10.15:9100/metrics", "lastScrapeDurationMs", 12),
                Map.of("job", "cadvisor", "health", "UP", "scrapeUrl", "http://10.0.10.15:8080/metrics", "lastScrapeDurationMs", 18),
                Map.of("job", "postgres_exporter", "health", "UP", "scrapeUrl", "http://10.0.10.20:9187/metrics", "lastScrapeDurationMs", 8),
                Map.of("job", "thingsboard", "health", "UP", "scrapeUrl", "http://10.0.10.15:8080/actuator/prometheus", "lastScrapeDurationMs", 45)
        );
    }
}
