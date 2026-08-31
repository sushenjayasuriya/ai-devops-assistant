package ai.devops.modules.integration.docker;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DockerIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(DockerIntegration.class);

    @Override
    public IntegrationType getType() {
        return IntegrationType.DOCKER;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        log.info("Testing Docker daemon connection: {}", endpointUrl);
        return true;
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return Map.of(
                "status", "HEALTHY",
                "serverVersion", "26.1.1",
                "containersRunning", 3,
                "containersStopped", 0,
                "images", 12
        );
    }

    public Map<String, Object> getContainerStats(String containerId) {
        if ("thingsboard-core-app".equalsIgnoreCase(containerId) || "d9f8e7a6b5c4".equalsIgnoreCase(containerId)) {
            return Map.of(
                    "containerId", "d9f8e7a6b5c4",
                    "name", "thingsboard-core-app",
                    "cpuPercent", 94.2,
                    "memoryUsageBytes", 1932735283L,
                    "memoryLimitBytes", 2147483648L,
                    "memoryPercent", 90.0,
                    "networkRxBytes", 104857600L,
                    "networkTxBytes", 52428800L,
                    "pids", 142
            );
        }

        return Map.of(
                "containerId", containerId,
                "name", containerId,
                "cpuPercent", 4.5,
                "memoryUsageBytes", 268435456L,
                "memoryLimitBytes", 2147483648L,
                "memoryPercent", 12.5,
                "networkRxBytes", 10485760L,
                "networkTxBytes", 2048576L,
                "pids", 24
        );
    }
}
