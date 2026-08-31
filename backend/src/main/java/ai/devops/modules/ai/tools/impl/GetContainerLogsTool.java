package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GetContainerLogsTool implements DevOpsTool {

    private final ContainerRepository containerRepository;
    private final ContainerService containerService;

    public GetContainerLogsTool(ContainerRepository containerRepository, ContainerService containerService) {
        this.containerRepository = containerRepository;
        this.containerService = containerService;
    }

    @Override
    public String getName() {
        return "get_container_logs";
    }

    @Override
    public String getDescription() {
        return "Fetch the latest logs from a container to diagnose application stack traces and errors.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "containerId", "string (required): Container name or ID",
                "tail", "integer (optional): Number of recent log lines to retrieve (default: 50)"
        );
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
        int tail = parameters.containsKey("tail") ? Integer.parseInt(String.valueOf(parameters.get("tail"))) : 50;

        Optional<ContainerEntity> containerOpt = containerRepository.findByName(containerId)
                .or(() -> containerRepository.findByContainerId(containerId));

        if (containerOpt.isEmpty()) {
            return ToolExecutionResult.error(getName(), "Container not found: " + containerId);
        }

        List<String> logs = containerService.getContainerLogs(containerOpt.get().getId(), tail);
        return ToolExecutionResult.ok(getName(), Map.of("container", containerId, "lines", logs));
    }
}
