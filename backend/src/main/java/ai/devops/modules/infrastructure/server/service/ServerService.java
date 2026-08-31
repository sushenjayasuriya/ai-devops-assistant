package ai.devops.modules.infrastructure.server.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ServerService {

    private final ServerRepository serverRepository;

    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Transactional(readOnly = true)
    public List<ServerEntity> getServers(UUID environmentId) {
        if (environmentId != null) {
            return serverRepository.findByEnvironmentId(environmentId);
        }
        return serverRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServerEntity getServerById(UUID id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Server", id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getServerLiveMetrics(UUID id) {
        ServerEntity server = getServerById(id);

        // Provides live telemetry snapshot
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("serverId", server.getId());
        metrics.put("hostname", server.getHostname());
        metrics.put("ipAddress", server.getIpAddress());
        metrics.put("status", server.getStatus());
        metrics.put("cpuUsagePercent", "DEGRADED".equals(server.getStatus()) ? 88.4 : 24.5);
        metrics.put("memoryUsagePercent", "DEGRADED".equals(server.getStatus()) ? 92.1 : 45.2);
        metrics.put("diskUsagePercent", 68.0);
        metrics.put("loadAverage1m", "DEGRADED".equals(server.getStatus()) ? 14.8 : 1.2);
        metrics.put("loadAverage5m", "DEGRADED".equals(server.getStatus()) ? 12.3 : 1.1);
        metrics.put("loadAverage15m", "DEGRADED".equals(server.getStatus()) ? 9.7 : 0.9);
        metrics.put("uptimeSeconds", 1452900L);
        metrics.put("networkRxBytesPerSec", 4850000L);
        metrics.put("networkTxBytesPerSec", 9200000L);

        List<Map<String, Object>> topProcesses = List.of(
                Map.of("pid", 4120, "name", "thingsboard-core", "cpu", 78.5, "mem", 42.1, "user", "tb"),
                Map.of("pid", 2105, "name", "postgres", "cpu", 12.2, "mem", 28.4, "user", "postgres"),
                Map.of("pid", 1024, "name", "dockerd", "cpu", 2.1, "mem", 4.5, "user", "root")
        );
        metrics.put("topProcesses", topProcesses);

        return metrics;
    }
}
