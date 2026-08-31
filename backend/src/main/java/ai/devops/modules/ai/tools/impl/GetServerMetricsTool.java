package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.modules.infrastructure.server.service.ServerService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class GetServerMetricsTool implements DevOpsTool {

    private final ServerRepository serverRepository;
    private final ServerService serverService;

    public GetServerMetricsTool(ServerRepository serverRepository, ServerService serverService) {
        this.serverRepository = serverRepository;
        this.serverService = serverService;
    }

    @Override
    public String getName() {
        return "get_server_metrics";
    }

    @Override
    public String getDescription() {
        return "Retrieve live CPU, Memory, Disk, Load average, and top running processes for a server.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("hostname", "string (required): Server hostname or IP address");
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.READ_ONLY;
    }

    @Override
    public Role getRequiredRole() {
        return Role.VIEWER;
    }

    @Override
    public boolean requiresProductionApproval() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        String hostname = String.valueOf(parameters.getOrDefault("hostname", "prod-core-node-01.acme.internal"));
        Optional<ServerEntity> serverOpt = serverRepository.findByHostname(hostname);

        if (serverOpt.isEmpty()) {
            serverOpt = serverRepository.findAll().stream().findFirst();
        }

        if (serverOpt.isEmpty()) {
            return ToolExecutionResult.error(getName(), "No servers found");
        }

        Map<String, Object> metrics = serverService.getServerLiveMetrics(serverOpt.get().getId());
        return ToolExecutionResult.ok(getName(), metrics);
    }
}
