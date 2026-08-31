package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.modules.integration.kubernetes.dto.K8sPodDto;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetKubernetesPodsTool implements DevOpsTool {

    private final KubernetesIntegration kubernetesIntegration;
    private final IntegrationRepository integrationRepository;

    public GetKubernetesPodsTool(KubernetesIntegration kubernetesIntegration, IntegrationRepository integrationRepository) {
        this.kubernetesIntegration = kubernetesIntegration;
        this.integrationRepository = integrationRepository;
    }

    @Override
    public String getName() {
        return "get_kubernetes_pods";
    }

    @Override
    public String getDescription() {
        return "List Kubernetes pods, their lifecycle phase, container status, and restart counts across namespaces.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "namespace", "string (optional): Kubernetes namespace (default: default)",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("namespace", "environment");
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
        String namespace = parameters.containsKey("namespace") ? String.valueOf(parameters.get("namespace")).trim() : "default";
        String configEncrypted = null;

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId != null) {
            List<IntegrationEntity> integrations = integrationRepository.findByOrganizationId(orgId);
            for (IntegrationEntity i : integrations) {
                if (i.getType() == IntegrationType.KUBERNETES && i.isEnabled()) {
                    configEncrypted = i.getConfigEncrypted();
                    break;
                }
            }
        }

        try {
            List<K8sPodDto> pods = kubernetesIntegration.getPods(configEncrypted, namespace);
            return ToolExecutionResult.ok(getName(), pods);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), "Failed to query Kubernetes pods: " + ex.getMessage());
        }
    }
}
