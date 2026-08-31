package ai.devops.modules.ai.tools.impl;

import ai.devops.common.exception.ApprovalRequiredException;
import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class RestartKubernetesDeploymentTool implements DevOpsTool {

    private final KubernetesIntegration kubernetesIntegration;
    private final IntegrationRepository integrationRepository;
    private final EnvironmentRepository environmentRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final UserRepository userRepository;

    public RestartKubernetesDeploymentTool(
            KubernetesIntegration kubernetesIntegration,
            IntegrationRepository integrationRepository,
            EnvironmentRepository environmentRepository,
            ApprovalWorkflowService approvalWorkflowService,
            UserRepository userRepository) {
        this.kubernetesIntegration = kubernetesIntegration;
        this.integrationRepository = integrationRepository;
        this.environmentRepository = environmentRepository;
        this.approvalWorkflowService = approvalWorkflowService;
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "restart_kubernetes_deployment";
    }

    @Override
    public String getDescription() {
        return "Perform a rolling restart of a Kubernetes Deployment. In PRODUCTION environments, this generates an approval request.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "deploymentName", "string (required): Name of the target Deployment",
                "namespace", "string (optional): Kubernetes namespace (default: default)",
                "environment", "string (optional): Target environment name"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of("deploymentName");
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("deploymentName", "namespace", "environment");
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.HIGH_RISK;
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
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        String deploymentName = String.valueOf(parameters.get("deploymentName")).trim();
        String namespace = parameters.containsKey("namespace") ? String.valueOf(parameters.get("namespace")).trim() : "default";

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return ToolExecutionResult.error(getName(), "User is not associated with an organization");
        }

        // Resolve target environment and integration
        IntegrationEntity k8sIntegration = null;
        List<IntegrationEntity> integrations = integrationRepository.findByOrganizationId(orgId);
        for (IntegrationEntity i : integrations) {
            if (i.getType() == IntegrationType.KUBERNETES && i.isEnabled()) {
                k8sIntegration = i;
                break;
            }
        }

        if (k8sIntegration == null) {
            return ToolExecutionResult.error(getName(), "No active Kubernetes integration configured for this organization");
        }

        EnvironmentEntity env = k8sIntegration.getEnvironment();
        boolean isProd = env.isProduction();

        // In Production: strictly require approval
        if (isProd) {
            String currentUserEmail = SecurityUtils.getCurrentUserEmail();
            UserEntity user = userRepository.findByEmail(currentUserEmail).orElse(null);

            ApprovalRequestEntity approval = approvalWorkflowService.createApprovalRequest(
                    null,
                    env,
                    user,
                    "restart_k8s_deployment",
                    "KUBERNETES_DEPLOYMENT",
                    k8sIntegration.getId() + ":" + namespace + "/" + deploymentName,
                    deploymentName,
                    String.format("{\"namespace\":\"%s\",\"deploymentName\":\"%s\",\"integrationId\":\"%s\"}", namespace, deploymentName, k8sIntegration.getId()),
                    String.format("Trigger rolling restart of Kubernetes Deployment '%s' in namespace '%s'", deploymentName, namespace),
                    "Rolling replacement of pods; transient pod churn during rollout",
                    Duration.ofHours(1)
            );

            throw new ApprovalRequiredException("restart_k8s_deployment", env.getName(), RiskLevel.HIGH_RISK, approval.getId());
        }

        // In Non-Production: Execute immediately
        try {
            Map<String, Object> result = kubernetesIntegration.restartDeployment(k8sIntegration.getConfigEncrypted(), namespace, deploymentName);
            return ToolExecutionResult.ok(getName(), result);
        } catch (Exception ex) {
            return ToolExecutionResult.error(getName(), "Failed to rollout restart deployment: " + ex.getMessage());
        }
    }
}
