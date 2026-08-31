package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.modules.infrastructure.server.service.ServerService;
import ai.devops.modules.integration.linux.LinuxServerIntegration;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetServerMetricsTool implements DevOpsTool {

    private final ServerRepository serverRepository;
    private final ServerService serverService;
    private final LinuxServerIntegration linuxIntegration;

    public GetServerMetricsTool(ServerRepository serverRepository, ServerService serverService, LinuxServerIntegration linuxIntegration) {
        this.serverRepository = serverRepository;
        this.serverService = serverService;
        this.linuxIntegration = linuxIntegration;
    }

    @Override
    public String getName() {
        return "get_server_metrics";
    }

    @Override
    public String getDescription() {
        return "Retrieve system telemetry (CPU %, memory %, disk utilization, load averages, top processes) from a server via SSH/metrics telemetry.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "serverId", "string (required): Server hostname or UUID",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("serverId");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("serverId", "environment");
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
        String serverId = String.valueOf(parameters.get("serverId")).trim();
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return ToolExecutionResult.error(getName(), "User is not associated with an organization");
        }

        Optional<ServerEntity> serverOpt = serverRepository.findByHostname(serverId)
                .or(() -> serverRepository.findByIdAndOrganizationId(tryParseUuid(serverId), orgId));

        if (serverOpt.isEmpty()) {
            return ToolExecutionResult.error(getName(), "Server not found: " + serverId);
        }

        ServerEntity server = serverOpt.get();

        // 1. If server has SSH connection configured, collect live SSH telemetry
        if (server.getIntegration() != null && server.getIntegration().isEnabled()) {
            try {
                LinuxServerTelemetry telemetry = linuxIntegration.collectTelemetry(
                        server.getIpAddress() + ":" + server.getSshPort(),
                        server.getSshCredentialEncrypted() != null ? server.getSshCredentialEncrypted() : server.getIntegration().getConfigEncrypted(),
                        5000
                );
                return ToolExecutionResult.ok(getName(), telemetry);
            } catch (Exception ignored) {}
        }

        // 2. Fallback to ServerService snapshot
        Map<String, Object> metrics = serverService.getServerLiveMetrics(server.getId());
        return ToolExecutionResult.ok(getName(), metrics);
    }

    private UUID tryParseUuid(String val) {
        try {
            return UUID.fromString(val);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }
}
