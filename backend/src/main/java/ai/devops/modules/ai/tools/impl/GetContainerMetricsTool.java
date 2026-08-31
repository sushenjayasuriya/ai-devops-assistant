package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.modules.integration.docker.dto.DockerContainerStats;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetContainerMetricsTool implements DevOpsTool {

    private final DockerIntegration dockerIntegration;
    private final IntegrationRepository integrationRepository;
    private final String defaultDockerHost;

    public GetContainerMetricsTool(
            DockerIntegration dockerIntegration,
            IntegrationRepository integrationRepository,
            @Value("${app.integrations.docker.default-host:tcp://localhost:2375}") String defaultDockerHost) {
        this.dockerIntegration = dockerIntegration;
        this.integrationRepository = integrationRepository;
        this.defaultDockerHost = defaultDockerHost;
    }

    @Override
    public String getName() {
        return "get_container_metrics";
    }

    @Override
    public String getDescription() {
        return "Retrieve real-time resource utilization (CPU %, Memory MB/limit, Network I/O, PIDs) for a Docker container.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "containerId", "string (required): Container name or ID",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("containerId");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("containerId", "environment");
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
        String containerId = String.valueOf(parameters.get("containerId")).trim();
        String dockerHost = defaultDockerHost;

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId != null) {
            List<IntegrationEntity> integrations = integrationRepository.findByOrganizationId(orgId);
            for (IntegrationEntity i : integrations) {
                if (i.getType() == IntegrationType.DOCKER && i.isEnabled()) {
                    dockerHost = i.getEndpointUrl();
                    break;
                }
            }
        }

        try {
            DockerContainerStats stats = dockerIntegration.getContainerStats(dockerHost, containerId);
            return ToolExecutionResult.ok(getName(), stats);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), "Failed to get container metrics: " + ex.getMessage());
        }
    }
}
