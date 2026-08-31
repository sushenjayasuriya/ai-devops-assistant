package ai.devops.modules.ai.tools;

import ai.devops.common.model.RiskLevel;
import ai.devops.security.rbac.Role;

import java.util.Map;

public interface DevOpsTool {
    String getName();
    String getDescription();
    Map<String, String> getParameterSchema();
    RiskLevel getRiskLevel();
    Role getRequiredRole();
    boolean requiresProductionApproval();
    ToolExecutionResult execute(Map<String, Object> parameters);
}
