package ai.devops.modules.approval.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.approval.dto.ResolveApprovalRequest;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.repository.ApprovalRequestRepository;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApprovalWorkflowService {

    private final ApprovalRequestRepository approvalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ApprovalWorkflowService(
            ApprovalRequestRepository approvalRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestEntity> getPendingApprovals() {
        return approvalRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestEntity> getApprovalsByEnvironment(UUID environmentId) {
        return approvalRepository.findByEnvironmentIdOrderByRequestedAtDesc(environmentId);
    }

    @Transactional
    public ApprovalRequestEntity createApprovalRequest(
            UUID aiActionId,
            EnvironmentEntity environment,
            UserEntity requestedByUser,
            String actionType,
            String rationale,
            String expectedImpact) {

        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setAiActionId(aiActionId);
        request.setEnvironment(environment);
        request.setRequestedByUser(requestedByUser);
        request.setActionType(actionType);
        request.setRationale(rationale);
        request.setExpectedImpact(expectedImpact);
        request.setStatus("PENDING");
        request.setRequestedAt(Instant.now());

        ApprovalRequestEntity saved = approvalRepository.save(request);

        auditService.recordAudit(
                "CREATE_APPROVAL_REQUEST",
                "APPROVAL",
                saved.getId().toString(),
                environment.getName(),
                RiskLevel.HIGH_RISK,
                String.format("actionType=%s", actionType),
                "PENDING",
                null,
                null
        );

        return saved;
    }

    @Transactional
    public ApprovalRequestEntity resolveApproval(UUID approvalId, ResolveApprovalRequest requestDto) {
        if (!SecurityUtils.isDevopsEngineer()) {
            throw new UnauthorizedActionException("Resolving approval requests requires DEVOPS_ENGINEER or ADMIN role");
        }

        ApprovalRequestEntity approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", approvalId));

        if (!"PENDING".equals(approval.getStatus())) {
            throw new IllegalStateException("Approval request has already been resolved with status: " + approval.getStatus());
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        UserEntity resolvingUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        approval.setStatus(requestDto.getDecision().toUpperCase());
        approval.setResolvedByUser(resolvingUser);
        approval.setResolvedAt(Instant.now());

        ApprovalRequestEntity updated = approvalRepository.save(approval);

        auditService.recordAudit(
                "RESOLVE_APPROVAL_REQUEST",
                "APPROVAL",
                updated.getId().toString(),
                updated.getEnvironment().getName(),
                RiskLevel.HIGH_RISK,
                String.format("decision=%s, comment=%s", requestDto.getDecision(), requestDto.getComment()),
                "APPROVED".equalsIgnoreCase(requestDto.getDecision()) ? "SUCCESS" : "REJECTED",
                requestDto.getComment(),
                null
        );

        return updated;
    }
}
