package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QueryPrometheusTool implements DevOpsTool {

    private final PrometheusIntegration prometheusIntegration;

    public QueryPrometheusTool(PrometheusIntegration prometheusIntegration) {
        this.prometheusIntegration = prometheusIntegration;
    }

    @Override
    public String getName() {
        return "query_prometheus";
    }

    @Override
    public String getDescription() {
        return "Execute a controlled PromQL expression to retrieve time-series metrics from Prometheus.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "query", "string (required): Safe PromQL expression (e.g. rate(http_requests_total[5m]))",
                "endpointUrl", "string (optional): Prometheus endpoint URL"
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
        String query = String.valueOf(parameters.getOrDefault("query", "container_cpu_usage_percent{container=\"thingsboard\"}"));
        String endpoint = String.valueOf(parameters.getOrDefault("endpointUrl", "http://localhost:9090"));

        Map<String, Object> result = prometheusIntegration.executePromQl(query, endpoint);
        return ToolExecutionResult.ok(getName(), result);
    }
}
