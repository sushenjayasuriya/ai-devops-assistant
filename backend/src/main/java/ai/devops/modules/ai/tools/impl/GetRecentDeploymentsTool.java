package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.service.DeploymentService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetRecentDeploymentsTool implements DevOpsTool {

    private final DeploymentService deploymentService;

    public GetRecentDeploymentsTool(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @Override
    public String getName() {
        return "get_recent_deployments";
    }

    @Override
    public String getDescription() {
        return "Retrieve the latest CI/CD deployment history, release versions, commits, and rollout statuses.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "serviceName", "string (optional): Target service name to filter deployment history",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("serviceName", "environment");
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
        String serviceFilter = parameters.containsKey("serviceName") ? String.valueOf(parameters.get("serviceName")).trim() : null;

        List<DeploymentEntity> deployments = deploymentService.getDeployments(null);
        if (serviceFilter != null && !serviceFilter.isBlank()) {
            deployments = deployments.stream()
                    .filter(d -> d.getServiceName().toLowerCase().contains(serviceFilter.toLowerCase()))
                    .toList();
        }

        List<Map<String, Object>> summary = deployments.stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("serviceName", d.getServiceName());
                    map.put("version", d.getVersionTag());
                    map.put("commitSha", d.getCommitSha());
                    map.put("status", d.getStatus());
                    map.put("deployedBy", d.getDeployedBy());
                    map.put("startedAt", d.getStartedAt());
                    map.put("completedAt", d.getCompletedAt());
                    return map;
                })
                .toList();

        return ToolExecutionResult.ok(getName(), summary);
    }
}
