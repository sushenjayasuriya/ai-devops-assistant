package ai.devops.modules.approval.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.approval.dto.ResolveApprovalRequest;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.repository.ApprovalRequestRepository;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApprovalWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflowService.class);

    private final ApprovalRequestRepository approvalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ContainerRepository containerRepository;
    private final ContainerService containerService;
    private final IntegrationRepository integrationRepository;
    private final KubernetesIntegration kubernetesIntegration;
    private final ObjectMapper objectMapper;

    public ApprovalWorkflowService(
            ApprovalRequestRepository approvalRepository,
            UserRepository userRepository,
            AuditService auditService,
            ContainerRepository containerRepository,
            @Lazy ContainerService containerService,
            IntegrationRepository integrationRepository,
            KubernetesIntegration kubernetesIntegration,
            ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.containerRepository = containerRepository;
        this.containerService = containerService;
        this.integrationRepository = integrationRepository;
        this.kubernetesIntegration = kubernetesIntegration;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestEntity> getPendingApprovals() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }
        return approvalRepository.findByOrganizationIdAndStatusOrderByRequestedAtDesc(orgId, "PENDING");
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestEntity> getApprovalsByEnvironment(UUID environmentId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }
        return approvalRepository.findByOrganizationIdAndEnvironmentIdOrderByRequestedAtDesc(orgId, environmentId);
    }

    @Transactional
    public ApprovalRequestEntity createApprovalRequest(
            UUID aiActionId,
            EnvironmentEntity environment,
            UserEntity requestedByUser,
            String actionType,
            String targetResourceType,
            String targetResourceId,
            String targetResourceName,
            String actionParameters,
            String rationale,
            String expectedImpact,
            Duration ttl) {

        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setAiActionId(aiActionId);
        request.setOrganization(environment.getOrganization());
        request.setEnvironment(environment);
        request.setRequestedByUser(requestedByUser);
        request.setActionType(actionType);
        request.setTargetResourceType(targetResourceType);
        request.setTargetResourceId(targetResourceId);
        request.setTargetResourceName(targetResourceName);
        request.setActionParameters(actionParameters);
        request.setRationale(rationale);
        request.setExpectedImpact(expectedImpact);
        request.setStatus("PENDING");
        request.setRequestedAt(Instant.now());
        request.setExpiresAt(Instant.now().plus(ttl != null ? ttl : Duration.ofHours(1)));

        ApprovalRequestEntity saved = approvalRepository.save(request);

        auditService.recordAudit(
                "CREATE_APPROVAL_REQUEST",
                "APPROVAL",
                saved.getId().toString(),
                environment.getName(),
                RiskLevel.HIGH_RISK,
                String.format("actionType=%s, target=%s:%s", actionType, targetResourceType, targetResourceId),
                "PENDING",
                null,
                null
        );

        return saved;
    }

    @Transactional
    public ApprovalRequestEntity resolveApproval(UUID approvalId, ResolveApprovalRequest requestDto) {
        if (!SecurityUtils.isDevopsEngineer()) {
            throw new UnauthorizedActionException("Resolving approval requests requires DEVOPS_ENGINEER or ADMIN role.");
        }

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedActionException("User is not associated with an active organization.");
        }

        ApprovalRequestEntity approval = approvalRepository.findByIdAndOrganizationId(approvalId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", approvalId));

        // State Machine Validations
        if (!"PENDING".equalsIgnoreCase(approval.getStatus())) {
            throw new IllegalStateException(String.format("Approval request cannot be resolved. Current status is already '%s'.", approval.getStatus()));
        }

        if (approval.isExpired()) {
            approval.setStatus("EXPIRED");
            approvalRepository.save(approval);
            throw new IllegalStateException("Approval request has expired and cannot be approved.");
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        UserEntity resolvingUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        String decision = requestDto.getDecision().toUpperCase();
        approval.setResolvedByUser(resolvingUser);
        approval.setResolvedAt(Instant.now());

        if ("REJECTED".equals(decision)) {
            approval.setStatus("REJECTED");
            approval.setExecutionResult("Rejected by approver: " + (requestDto.getComment() != null ? requestDto.getComment() : "No comment provided"));
            ApprovalRequestEntity saved = approvalRepository.save(approval);

            auditService.recordAudit(
                    "RESOLVE_APPROVAL_REQUEST:REJECT",
                    "APPROVAL",
                    saved.getId().toString(),
                    saved.getEnvironment().getName(),
                    RiskLevel.HIGH_RISK,
                    String.format("decision=REJECTED, comment=%s", requestDto.getComment()),
                    "REJECTED",
                    requestDto.getComment(),
                    null
            );

            return saved;
        }

        if ("APPROVED".equals(decision)) {
            approval.setStatus("APPROVED");

            // Execute the approved action immediately and atomically
            try {
                String executionResult = executeAction(approval);
                approval.setStatus("EXECUTED");
                approval.setExecutedAt(Instant.now());
                approval.setExecutionResult(executionResult);

                ApprovalRequestEntity saved = approvalRepository.save(approval);

                auditService.recordAudit(
                    "EXECUTE_APPROVED_ACTION",
                    approval.getTargetResourceType() != null ? approval.getTargetResourceType() : "INFRASTRUCTURE",
                    approval.getTargetResourceId() != null ? approval.getTargetResourceId() : saved.getId().toString(),
                    saved.getEnvironment().getName(),
                    RiskLevel.HIGH_RISK,
                    String.format("actionType=%s, params=%s", saved.getActionType(), saved.getActionParameters()),
                    "SUCCESS",
                    null,
                    null
                );

                return saved;
            } catch (Exception ex) {
                log.error("Failed to execute approved action [{}] for approval [{}]", approval.getActionType(), approvalId, ex);
                approval.setExecutionResult("Execution failed: " + ex.getMessage());
                ApprovalRequestEntity saved = approvalRepository.save(approval);

                auditService.recordAudit(
                    "EXECUTE_APPROVED_ACTION:FAILED",
                    approval.getTargetResourceType() != null ? approval.getTargetResourceType() : "INFRASTRUCTURE",
                    approval.getTargetResourceId() != null ? approval.getTargetResourceId() : saved.getId().toString(),
                    saved.getEnvironment().getName(),
                    RiskLevel.HIGH_RISK,
                    String.format("actionType=%s, error=%s", saved.getActionType(), ex.getMessage()),
                    "FAILURE",
                    ex.getMessage(),
                    null
                );

                throw new RuntimeException("Action execution failed after approval: " + ex.getMessage(), ex);
            }
        }

        throw new IllegalArgumentException("Invalid approval decision: " + decision + ". Expected APPROVED or REJECTED.");
    }

    private String executeAction(ApprovalRequestEntity approval) {
        String actionType = approval.getActionType().toLowerCase();
        String resourceType = approval.getTargetResourceType();

        if ("CONTAINER".equalsIgnoreCase(resourceType) || actionType.contains("container")) {
            UUID containerId = UUID.fromString(approval.getTargetResourceId());
            ContainerEntity container = containerRepository.findById(containerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Container", containerId));

            String action = actionType.replace("_container", "").trim();
            Map<String, Object> result = containerService.executeContainerStateChange(container, action);
            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                return result.toString();
            }
        }

        if ("KUBERNETES_DEPLOYMENT".equalsIgnoreCase(resourceType) || actionType.contains("k8s")) {
            try {
                JsonNode params = objectMapper.readTree(approval.getActionParameters());
                String namespace = params.path("namespace").asText("default");
                String deploymentName = params.path("deploymentName").asText();
                UUID integrationId = UUID.fromString(params.path("integrationId").asText());

                IntegrationEntity integration = integrationRepository.findById(integrationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Integration", integrationId));

                Map<String, Object> result = kubernetesIntegration.restartDeployment(
                        integration.getConfigEncrypted(), namespace, deploymentName);
                return objectMapper.writeValueAsString(result);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to execute Kubernetes rollout restart: " + ex.getMessage(), ex);
            }
        }

        return String.format("Action '%s' on %s (%s) executed successfully.", actionType, resourceType, approval.getTargetResourceId());
    }
}
