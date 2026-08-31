package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetContainerStatusTool implements DevOpsTool {

    private final ContainerRepository containerRepository;

    public GetContainerStatusTool(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Override
    public String getName() {
        return "get_container_status";
    }

    @Override
    public String getDescription() {
        return "Retrieve the status, state (RUNNING, RESTARTING, EXITED), restart counts, and image versions of containers.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("containerId", "string (optional): Container ID or name");
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
        String containerId = parameters != null && parameters.containsKey("containerId") ?
                String.valueOf(parameters.get("containerId")) : null;

        if (containerId != null && !containerId.isBlank()) {
            return containerRepository.findByName(containerId)
                    .or(() -> containerRepository.findByContainerId(containerId))
                    .map(c -> ToolExecutionResult.ok(getName(), Map.<String, Object>of(
                            "id", c.getId(),
                            "name", c.getName(),
                            "containerId", c.getContainerId(),
                            "image", c.getImage(),
                            "state", c.getState(),
                            "restartCount", c.getRestartCount(),
                            "environment", c.getEnvironment().getName()
                    )))
                    .orElseGet(() -> ToolExecutionResult.error(getName(), "Container not found: " + containerId));
        }

        List<ContainerEntity> containers = containerRepository.findAll();
        List<Map<String, Object>> result = containers.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "containerId", c.getContainerId(),
                        "image", c.getImage(),
                        "state", c.getState(),
                        "restartCount", c.getRestartCount(),
                        "environment", c.getEnvironment().getName()
                ))
                .toList();

        return ToolExecutionResult.ok(getName(), result);
    }
}
