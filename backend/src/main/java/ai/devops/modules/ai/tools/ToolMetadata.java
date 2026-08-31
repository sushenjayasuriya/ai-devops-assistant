package ai.devops.modules.ai.tools;

import ai.devops.common.model.RiskLevel;
import ai.devops.security.rbac.Role;

import java.util.Map;

public class ToolMetadata {
    private String name;
    private String description;
    private Map<String, String> parameterSchema;
    private RiskLevel riskLevel;
    private Role requiredRole;
    private boolean requiresProductionApproval;

    public ToolMetadata() {}

    public ToolMetadata(String name, String description, Map<String, String> parameterSchema, RiskLevel riskLevel, Role requiredRole, boolean requiresProductionApproval) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.riskLevel = riskLevel;
        this.requiredRole = requiredRole;
        this.requiresProductionApproval = requiresProductionApproval;
    }

    public static ToolMetadata from(DevOpsTool tool) {
        return new ToolMetadata(
                tool.getName(),
                tool.getDescription(),
                tool.getParameterSchema(),
                tool.getRiskLevel(),
                tool.getRequiredRole(),
                tool.requiresProductionApproval()
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getParameterSchema() {
        return parameterSchema;
    }

    public void setParameterSchema(Map<String, String> parameterSchema) {
        this.parameterSchema = parameterSchema;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Role getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(Role requiredRole) {
        this.requiredRole = requiredRole;
    }

    public boolean isRequiresProductionApproval() {
        return requiresProductionApproval;
    }

    public void setRequiresProductionApproval(boolean requiresProductionApproval) {
        this.requiresProductionApproval = requiresProductionApproval;
    }
}
