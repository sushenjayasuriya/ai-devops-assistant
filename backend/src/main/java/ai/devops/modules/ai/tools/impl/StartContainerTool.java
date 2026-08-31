package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StartContainerTool implements DevOpsTool {

    private final ContainerRepository containerRepository;
    private final ContainerService containerService;

    public StartContainerTool(ContainerRepository containerRepository, ContainerService containerService) {
        this.containerRepository = containerRepository;
        this.containerService = containerService;
    }

    @Override
    public String getName() {
        return "start_container";
    }

    @Override
    public String getDescription() {
        return "Start a stopped Docker container.";
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
        return RiskLevel.LOW_RISK;
    }

    @Override
    public Role getRequiredRole() {
        return Role.DEVOPS_ENGINEER;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean requiresProductionApproval() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        String containerId = String.valueOf(parameters.get("containerId")).trim();
        UUID orgId = SecurityUtils.getCurrentOrganizationId();

        Optional<ContainerEntity> containerOpt = Optional.empty();
        if (orgId != null) {
            containerOpt = containerRepository.findByNameAndOrganizationId(containerId, orgId)
                    .or(() -> containerRepository.findByIdAndOrganizationId(tryParseUuid(containerId), orgId));
        }

        if (containerOpt.isEmpty()) {
            return ToolExecutionResult.error(getName(), "Container not found in organization inventory: " + containerId);
        }

        try {
            Map<String, Object> result = containerService.requestOrExecuteContainerAction(containerOpt.get().getId(), "start");
            return ToolExecutionResult.ok(getName(), result);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), ex.getMessage());
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
