package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.repository.DeploymentRepository;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetRecentDeploymentsTool implements DevOpsTool {

    private final DeploymentRepository deploymentRepository;

    public GetRecentDeploymentsTool(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Override
    public String getName() {
        return "get_recent_deployments";
    }

    @Override
    public String getDescription() {
        return "Retrieve recent CI/CD deployments, commit SHAs, author, and changelogs to correlate with incidents.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("serviceName", "string (optional): Target service name (e.g. thingsboard-core-app)");
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
        String serviceName = parameters != null && parameters.containsKey("serviceName") ?
                String.valueOf(parameters.get("serviceName")) : null;

        List<DeploymentEntity> deployments;
        if (serviceName != null && !serviceName.isBlank()) {
            deployments = deploymentRepository.findByServiceNameOrderByStartedAtDesc(serviceName);
        } else {
            deployments = deploymentRepository.findAllByOrderByStartedAtDesc();
        }

        List<Map<String, Object>> result = deployments.stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "serviceName", d.getServiceName(),
                        "versionTag", d.getVersionTag(),
                        "commitSha", d.getCommitSha() != null ? d.getCommitSha() : "HEAD",
                        "deployedBy", d.getDeployedBy() != null ? d.getDeployedBy() : "ci-bot",
                        "status", d.getStatus(),
                        "changelog", d.getChangelog() != null ? d.getChangelog() : "",
                        "startedAt", d.getStartedAt()
                ))
                .toList();

        return ToolExecutionResult.ok(getName(), result);
    }
}
