package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetContainerLogsTool implements DevOpsTool {

    private final ContainerService containerService;
    private final ContainerRepository containerRepository;
    private final DockerIntegration dockerIntegration;
    private final IntegrationRepository integrationRepository;
    private final String defaultDockerHost;

    public GetContainerLogsTool(
            ContainerService containerService,
            ContainerRepository containerRepository,
            DockerIntegration dockerIntegration,
            IntegrationRepository integrationRepository,
            @Value("${app.integrations.docker.default-host:tcp://localhost:2375}") String defaultDockerHost) {
        this.containerService = containerService;
        this.containerRepository = containerRepository;
        this.dockerIntegration = dockerIntegration;
        this.integrationRepository = integrationRepository;
        this.defaultDockerHost = defaultDockerHost;
    }

    @Override
    public String getName() {
        return "get_container_logs";
    }

    @Override
    public String getDescription() {
        return "Fetch the latest console standard output (stdout/stderr) and stack traces from a Docker container.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "containerId", "string (required): Container name or ID",
                "tail", "integer (optional): Number of trailing log lines to fetch (default: 50)",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("containerId");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("containerId", "tail", "environment");
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
        int tail = 50;
        if (parameters.containsKey("tail")) {
            try {
                tail = Integer.parseInt(String.valueOf(parameters.get("tail")));
            } catch (NumberFormatException ignored) {}
        }

        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        // 1. Try querying real Docker daemon
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
            List<String> logs = dockerIntegration.getContainerLogs(dockerHost, containerId, tail);
            if (!logs.isEmpty()) {
                return ToolExecutionResult.ok(getName(), Map.of("containerId", containerId, "lines", logs.size(), "logs", logs));
            }
        } catch (Exception ignored) {}

        // 2. Query ContainerService log stream
        if (orgId != null) {
            Optional<ContainerEntity> containerOpt = containerRepository.findByNameAndOrganizationId(containerId, orgId)
                    .or(() -> containerRepository.findByIdAndOrganizationId(tryParseUuid(containerId), orgId));

            if (containerOpt.isPresent()) {
                List<String> logs = containerService.getContainerLogs(containerOpt.get().getId(), tail);
                return ToolExecutionResult.ok(getName(), Map.of("containerId", containerId, "lines", logs.size(), "logs", logs));
            }
        }

        return ToolExecutionResult.error(getName(), "Container not found or logs unavailable for: " + containerId);
    }

    private UUID tryParseUuid(String val) {
        try {
            return UUID.fromString(val);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }
}
