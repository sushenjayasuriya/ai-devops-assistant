package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetServerStatusTool implements DevOpsTool {

    private final ServerRepository serverRepository;

    public GetServerStatusTool(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    @Override
    public String getName() {
        return "get_server_status";
    }

    @Override
    public String getDescription() {
        return "Retrieve the status, hostnames, and IP addresses of all Linux servers in an environment.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("environment", "string (optional): Environment name (DEVELOPMENT, STAGING, PRODUCTION)");
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
        List<ServerEntity> servers = serverRepository.findAll();
        List<Map<String, Object>> result = servers.stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "hostname", s.getHostname(),
                        "ipAddress", s.getIpAddress(),
                        "status", s.getStatus(),
                        "osInfo", s.getOsInfo() != null ? s.getOsInfo() : "Linux",
                        "environment", s.getEnvironment() != null ? s.getEnvironment().getName() : "PRODUCTION"
                ))
                .toList();

        return ToolExecutionResult.ok(getName(), result);
    }
}
