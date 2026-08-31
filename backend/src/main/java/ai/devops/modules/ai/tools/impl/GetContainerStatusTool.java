package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.modules.integration.docker.dto.DockerContainerDetails;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetContainerStatusTool implements DevOpsTool {

    private final ContainerRepository containerRepository;
    private final DockerIntegration dockerIntegration;
    private final IntegrationRepository integrationRepository;
    private final String defaultDockerHost;

    public GetContainerStatusTool(
            ContainerRepository containerRepository,
            DockerIntegration dockerIntegration,
            IntegrationRepository integrationRepository,
            @Value("${app.integrations.docker.default-host:tcp://localhost:2375}") String defaultDockerHost) {
        this.containerRepository = containerRepository;
        this.dockerIntegration = dockerIntegration;
        this.integrationRepository = integrationRepository;
        this.defaultDockerHost = defaultDockerHost;
    }

    @Override
    public String getName() {
        return "get_container_status";
    }

    @Override
    public String getDescription() {
        return "Inspect live status, lifecycle state (running/restarting/exited), image, and restart count for a Docker container.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "containerId", "string (required): Container name or ID",
                "environment", "string (optional): Environment name"
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
        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        // 1. Check local repository within tenant
        Optional<ContainerEntity> entityOpt = Optional.empty();
        if (orgId != null) {
            entityOpt = containerRepository.findByNameAndOrganizationId(containerId, orgId)
                    .or(() -> containerRepository.findByIdAndOrganizationId(tryParseUuid(containerId), orgId));
        }

        // 2. Query real Docker daemon
        String dockerHost = defaultDockerHost;
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
            DockerContainerDetails details = dockerIntegration.inspectContainer(dockerHost, containerId);
            return ToolExecutionResult.ok(getName(), details);
        } catch (Exception ex) {
            // Fallback to database entity state if daemon is offline
            if (entityOpt.isPresent()) {
                ContainerEntity c = entityOpt.get();
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("id", c.getContainerId());
                fallback.put("name", c.getName());
                fallback.put("image", c.getImage());
                fallback.put("state", c.getState());
                fallback.put("restartCount", c.getRestartCount());
                fallback.put("note", "Retrieved from inventory cache (daemon unreachable: " + ex.getMessage() + ")");
                return ToolExecutionResult.ok(getName(), fallback);
            }
            return ToolExecutionResult.error(getName(), "Failed to inspect container status: " + ex.getMessage());
        }
    }

    private UUID tryParseUuid(String val) {
        try {
            return UUID.fromString(val);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }
}
