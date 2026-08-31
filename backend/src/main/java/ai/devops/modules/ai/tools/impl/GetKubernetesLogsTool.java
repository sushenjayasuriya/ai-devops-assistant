package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetKubernetesLogsTool implements DevOpsTool {

    private final KubernetesIntegration kubernetesIntegration;
    private final IntegrationRepository integrationRepository;

    public GetKubernetesLogsTool(KubernetesIntegration kubernetesIntegration, IntegrationRepository integrationRepository) {
        this.kubernetesIntegration = kubernetesIntegration;
        this.integrationRepository = integrationRepository;
    }

    @Override
    public String getName() {
        return "get_kubernetes_logs";
    }

    @Override
    public String getDescription() {
        return "Fetch container console logs and stack traces from a Kubernetes pod.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "podName", "string (required): Name of the target pod",
                "namespace", "string (optional): Kubernetes namespace (default: default)",
                "containerName", "string (optional): Target container inside the pod",
                "tail", "integer (optional): Number of trailing log lines (default: 50)"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("podName");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("podName", "namespace", "containerName", "tail", "environment");
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
        String podName = String.valueOf(parameters.get("podName")).trim();
        String namespace = parameters.containsKey("namespace") ? String.valueOf(parameters.get("namespace")).trim() : "default";
        String containerName = parameters.containsKey("containerName") ? String.valueOf(parameters.get("containerName")).trim() : null;
        int tail = 50;
        if (parameters.containsKey("tail")) {
            try {
                tail = Integer.parseInt(String.valueOf(parameters.get("tail")));
            } catch (NumberFormatException ignored) {}
        }

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
            List<String> logs = kubernetesIntegration.getPodLogs(configEncrypted, namespace, podName, containerName, tail);
            return ToolExecutionResult.ok(getName(), Map.of("podName", podName, "namespace", namespace, "lines", logs.size(), "logs", logs));
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), "Failed to read Kubernetes pod logs: " + ex.getMessage());
        }
    }
}
