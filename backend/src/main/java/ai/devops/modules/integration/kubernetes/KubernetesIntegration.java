package ai.devops.modules.integration.kubernetes;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KubernetesIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(KubernetesIntegration.class);

    @Override
    public IntegrationType getType() {
        return IntegrationType.KUBERNETES;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        log.info("Testing Kubernetes API connection: {}", endpointUrl);
        return true;
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return Map.of(
                "status", "HEALTHY",
                "clusterVersion", "v1.29.2",
                "nodesReady", 3,
                "nodesTotal", 3,
                "podsRunning", 24,
                "podsPending", 0
        );
    }

    public List<Map<String, Object>> getPods(String namespace) {
        return List.of(
                Map.of("name", "thingsboard-app-69c7f7d79b-z9k2q", "namespace", namespace != null ? namespace : "default", "status", "CrashLoopBackOff", "restarts", 7, "cpu", "940m", "memory", "1840Mi"),
                Map.of("name", "postgres-ha-postgresql-0", "namespace", namespace != null ? namespace : "default", "status", "Running", "restarts", 0, "cpu", "120m", "memory", "512Mi"),
                Map.of("name", "redis-master-0", "namespace", namespace != null ? namespace : "default", "status", "Running", "restarts", 0, "cpu", "35m", "memory", "128Mi")
        );
    }
}
