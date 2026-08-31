package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class StopContainerTool implements DevOpsTool {

    private final ContainerRepository containerRepository;
    private final ContainerService containerService;

    public StopContainerTool(ContainerRepository containerRepository, ContainerService containerService) {
        this.containerRepository = containerRepository;
        this.containerService = containerService;
    }

    @Override
    public String getName() {
        return "stop_container";
    }

    @Override
    public String getDescription() {
        return "Stop a running Docker container. Always requires human approval in STAGING and PRODUCTION.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "containerId", "string (required): Container name or ID",
                "approved", "boolean (optional): Whether explicit human approval was granted"
        );
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.HIGH_RISK;
    }

    @Override
    public Role getRequiredRole() {
        return Role.DEVOPS_ENGINEER;
    }

    @Override
    public boolean requiresProductionApproval() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        String containerId = String.valueOf(parameters.getOrDefault("containerId", ""));
        boolean approved = Boolean.parseBoolean(String.valueOf(parameters.getOrDefault("approved", "false")));

        Optional<ContainerEntity> containerOpt = containerRepository.findByName(containerId)
                .or(() -> containerRepository.findByContainerId(containerId));

        if (containerOpt.isEmpty()) {
            return ToolExecutionResult.error(getName(), "Container not found: " + containerId);
        }

        try {
            Map<String, Object> result = containerService.executeContainerAction(containerOpt.get().getId(), "stop", approved);
            return ToolExecutionResult.ok(getName(), result);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), ex.getMessage());
        }
    }
}
