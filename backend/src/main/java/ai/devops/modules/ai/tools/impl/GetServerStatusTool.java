package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;

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
        return "Retrieve the infrastructure status and operational health of Linux host nodes.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("environment");
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
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean requiresProductionApproval() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return ToolExecutionResult.ok(getName(), List.of());
        }

        List<ServerEntity> servers = serverRepository.findByOrganizationId(orgId);
        List<Map<String, Object>> summary = servers.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("hostname", s.getHostname());
                    map.put("ipAddress", s.getIpAddress());
                    map.put("status", s.getStatus());
                    map.put("environment", s.getEnvironment() != null ? s.getEnvironment().getName() : "UNKNOWN");
                    return map;
                })
                .toList();

        return ToolExecutionResult.ok(getName(), summary);
    }
}
