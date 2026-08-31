package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class QueryPrometheusTool implements DevOpsTool {

    private final PrometheusIntegration prometheusIntegration;
    private final IntegrationRepository integrationRepository;
    private final String defaultPrometheusUrl;

    public QueryPrometheusTool(
            PrometheusIntegration prometheusIntegration,
            IntegrationRepository integrationRepository,
            @Value("${app.integrations.prometheus.default-url:http://localhost:9090}") String defaultPrometheusUrl) {
        this.prometheusIntegration = prometheusIntegration;
        this.integrationRepository = integrationRepository;
        this.defaultPrometheusUrl = defaultPrometheusUrl;
    }

    @Override
    public String getName() {
        return "query_prometheus";
    }

    @Override
    public String getDescription() {
        return "Execute an instant PromQL query against Prometheus to retrieve live metric timeseries data.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "query", "string (required): The PromQL expression to evaluate",
                "environment", "string (optional): Environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("query");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("query", "environment");
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
        String query = String.valueOf(parameters.get("query")).trim();
        String endpointUrl = defaultPrometheusUrl;
        String configEncrypted = null;

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId != null) {
            List<IntegrationEntity> integrations = integrationRepository.findByOrganizationId(orgId);
            for (IntegrationEntity i : integrations) {
                if (i.getType() == IntegrationType.PROMETHEUS && i.isEnabled()) {
                    endpointUrl = i.getEndpointUrl();
                    configEncrypted = i.getConfigEncrypted();
                    break;
                }
            }
        }

        try {
            PrometheusQueryResponse response = prometheusIntegration.executePromQl(query, endpointUrl, configEncrypted, Instant.now(), 5000);
            return ToolExecutionResult.ok(getName(), response.getData() != null ? response.getData() : Map.of("status", response.getStatus()));
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), ex.getMessage());
        }
    }
}
