package ai.devops.modules.ai.tools;

import ai.devops.common.model.RiskLevel;
import ai.devops.security.rbac.Role;

import java.util.Map;
import java.util.Set;

public interface DevOpsTool {
    String getName();
    String getDescription();
    Map<String, String> getParameterSchema();
    Set<String> getRequiredParameters();
    Set<String> getAllowedParameters();
    RiskLevel getRiskLevel();
    Role getRequiredRole();
    boolean isReadOnly();
    boolean requiresProductionApproval();

    default void validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            parameters = Map.of();
        }

        // Check required parameters
        for (String req : getRequiredParameters()) {
            if (!parameters.containsKey(req) || parameters.get(req) == null || String.valueOf(parameters.get(req)).isBlank()) {
                throw new IllegalArgumentException(String.format("Tool '%s' missing required parameter: '%s'", getName(), req));
            }
        }

        // Check unexpected parameters (strict safety contract)
        Set<String> allowed = getAllowedParameters();
        for (String key : parameters.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException(String.format("Tool '%s' received forbidden/unknown parameter: '%s'", getName(), key));
            }
        }
    }

    ToolExecutionResult execute(Map<String, Object> parameters);
}
