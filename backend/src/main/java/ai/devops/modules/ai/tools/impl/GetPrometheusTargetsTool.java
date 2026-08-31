package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetPrometheusTargetsTool implements DevOpsTool {

    private final PrometheusIntegration prometheusIntegration;

    public GetPrometheusTargetsTool(PrometheusIntegration prometheusIntegration) {
        this.prometheusIntegration = prometheusIntegration;
    }

    @Override
    public String getName() {
        return "get_prometheus_targets";
    }

    @Override
    public String getDescription() {
        return "Retrieve the status and health of all configured Prometheus scrape targets.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("endpointUrl", "string (optional): Prometheus server URL");
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
        String endpoint = String.valueOf(parameters.getOrDefault("endpointUrl", "http://localhost:9090"));
        List<Map<String, Object>> targets = prometheusIntegration.getTargets(endpoint);
        return ToolExecutionResult.ok(getName(), targets);
    }
}
