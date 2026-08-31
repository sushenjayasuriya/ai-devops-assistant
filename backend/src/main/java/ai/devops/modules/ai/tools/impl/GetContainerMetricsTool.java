package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetContainerMetricsTool implements DevOpsTool {

    private final DockerIntegration dockerIntegration;

    public GetContainerMetricsTool(DockerIntegration dockerIntegration) {
        this.dockerIntegration = dockerIntegration;
    }

    @Override
    public String getName() {
        return "get_container_metrics";
    }

    @Override
    public String getDescription() {
        return "Retrieve real-time CPU %, Memory usage, Network I/O, and PIDs for a specific container.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("containerId", "string (required): Container name or hash ID");
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
        String containerId = String.valueOf(parameters.getOrDefault("containerId", "thingsboard-core-app"));
        Map<String, Object> stats = dockerIntegration.getContainerStats(containerId);
        return ToolExecutionResult.ok(getName(), stats);
    }
}
