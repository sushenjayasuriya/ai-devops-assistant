package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import ai.devops.modules.integration.prometheus.dto.PrometheusTargetsResponse;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetPrometheusTargetsTool implements DevOpsTool {

    private final PrometheusIntegration prometheusIntegration;
    private final IntegrationRepository integrationRepository;
    private final String defaultPrometheusUrl;

    public GetPrometheusTargetsTool(
            PrometheusIntegration prometheusIntegration,
            IntegrationRepository integrationRepository,
            @Value("${app.integrations.prometheus.default-url:http://localhost:9090}") String defaultPrometheusUrl) {
        this.prometheusIntegration = prometheusIntegration;
        this.integrationRepository = integrationRepository;
        this.defaultPrometheusUrl = defaultPrometheusUrl;
    }

    @Override
    public String getName() {
        return "get_prometheus_targets";
    }

    @Override
    public String getDescription() {
        return "Retrieve the list of active and dropped scrape targets in Prometheus and their health status (UP/DOWN).";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("environment");
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
            PrometheusTargetsResponse response = prometheusIntegration.getTargets(endpointUrl, configEncrypted, 5000);
            return ToolExecutionResult.ok(getName(), response.getData() != null ? response.getData() : response);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), ex.getMessage());
        }
    }
}
