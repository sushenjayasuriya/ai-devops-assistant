package ai.devops.modules.approval;

import ai.devops.modules.approval.dto.ResolveApprovalRequest;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.repository.ApprovalRequestRepository;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowTest {

    @Mock
    private ApprovalRequestRepository approvalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private ContainerService containerService;

    @Mock
    private IntegrationRepository integrationRepository;

    @Mock
    private KubernetesIntegration kubernetesIntegration;

    private ApprovalWorkflowService approvalWorkflowService;

    private OrganizationEntity org;
    private UserEntity approver;
    private EnvironmentEntity prodEnv;
    private ContainerEntity targetContainer;
    private ApprovalRequestEntity pendingApproval;

    @BeforeEach
    void setUp() {
        approvalWorkflowService = new ApprovalWorkflowService(
                approvalRepository,
                userRepository,
                auditService,
                containerRepository,
                containerService,
                integrationRepository,
                kubernetesIntegration,
                new ObjectMapper()
        );

        org = new OrganizationEntity("Acme Corp", "acme-corp");
        org.setId(UUID.randomUUID());

        approver = new UserEntity();
        approver.setId(UUID.randomUUID());
        approver.setEmail("lead-sre@devops.ai");
        approver.setOrganization(org);
        approver.setRoles(Set.of("DEVOPS_ENGINEER"));
        approver.setEnabled(true);

        prodEnv = new EnvironmentEntity(org, "PRODUCTION", "Production", true);
        prodEnv.setId(UUID.randomUUID());

        targetContainer = new ContainerEntity();
        targetContainer.setId(UUID.randomUUID());
        targetContainer.setName("thingsboard-core-app");
        targetContainer.setEnvironment(prodEnv);
        targetContainer.setState("RESTARTING");

        pendingApproval = new ApprovalRequestEntity();
        pendingApproval.setId(UUID.randomUUID());
        pendingApproval.setOrganization(org);
        pendingApproval.setEnvironment(prodEnv);
        pendingApproval.setRequestedByUser(approver);
        pendingApproval.setActionType("restart_container");
        pendingApproval.setTargetResourceType("CONTAINER");
        pendingApproval.setTargetResourceId(targetContainer.getId().toString());
        pendingApproval.setTargetResourceName("thingsboard-core-app");
        pendingApproval.setActionParameters(String.format("{\"containerId\":\"%s\"}", targetContainer.getId()));
        pendingApproval.setRationale("Clear hung thread locks in database pool");
        pendingApproval.setExpectedImpact("Transient 5s delay");
        pendingApproval.setStatus("PENDING");
        pendingApproval.setRequestedAt(Instant.now());
        pendingApproval.setExpiresAt(Instant.now().plusSeconds(3600));

        CustomUserDetails userDetails = new CustomUserDetails(approver);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    @DisplayName("Approving a request must execute the underlying action and transition to EXECUTED")
    void testApproveExecutesActionSuccessfully() {
        when(approvalRepository.findByIdAndOrganizationId(pendingApproval.getId(), org.getId()))
                .thenReturn(Optional.of(pendingApproval));
        when(userRepository.findByEmail(approver.getEmail())).thenReturn(Optional.of(approver));
        when(containerRepository.findById(targetContainer.getId())).thenReturn(Optional.of(targetContainer));
        when(containerService.executeContainerStateChange(eq(targetContainer), eq("restart")))
                .thenReturn(Map.of("status", "SUCCESS", "newState", "RUNNING"));
        when(approvalRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(i -> i.getArgument(0));

        ResolveApprovalRequest request = new ResolveApprovalRequest("APPROVED", "Verified alert root cause");
        ApprovalRequestEntity result = approvalWorkflowService.resolveApproval(pendingApproval.getId(), request);

        assertNotNull(result);
        assertEquals("EXECUTED", result.getStatus());
        assertNotNull(result.getExecutedAt());
        assertNotNull(result.getResolvedAt());
        verify(containerService, times(1)).executeContainerStateChange(targetContainer, "restart");
    }

    @Test
    @DisplayName("Rejecting a request transitions status to REJECTED and does not execute the action")
    void testRejectApprovalDoesNotExecuteAction() {
        when(approvalRepository.findByIdAndOrganizationId(pendingApproval.getId(), org.getId()))
                .thenReturn(Optional.of(pendingApproval));
        when(userRepository.findByEmail(approver.getEmail())).thenReturn(Optional.of(approver));
        when(approvalRepository.save(any(ApprovalRequestEntity.class))).thenAnswer(i -> i.getArgument(0));

        ResolveApprovalRequest request = new ResolveApprovalRequest("REJECTED", "False positive alert");
        ApprovalRequestEntity result = approvalWorkflowService.resolveApproval(pendingApproval.getId(), request);

        assertEquals("REJECTED", result.getStatus());
        assertNull(result.getExecutedAt());
        verify(containerService, never()).executeContainerStateChange(any(), any());
    }

    @Test
    @DisplayName("Cannot resolve an already resolved/executed approval request")
    void testCannotReResolveApproval() {
        pendingApproval.setStatus("EXECUTED");
        when(approvalRepository.findByIdAndOrganizationId(pendingApproval.getId(), org.getId()))
                .thenReturn(Optional.of(pendingApproval));

        ResolveApprovalRequest request = new ResolveApprovalRequest("APPROVED", "Try execute again");
        assertThrows(IllegalStateException.class, () ->
                approvalWorkflowService.resolveApproval(pendingApproval.getId(), request)
        );
        verify(containerService, never()).executeContainerStateChange(any(), any());
    }

    @Test
    @DisplayName("Cannot approve an expired approval request")
    void testCannotApproveExpiredRequest() {
        pendingApproval.setExpiresAt(Instant.now().minusSeconds(60)); // Expired in past
        when(approvalRepository.findByIdAndOrganizationId(pendingApproval.getId(), org.getId()))
                .thenReturn(Optional.of(pendingApproval));

        ResolveApprovalRequest request = new ResolveApprovalRequest("APPROVED", "Late approval");
        assertThrows(IllegalStateException.class, () ->
                approvalWorkflowService.resolveApproval(pendingApproval.getId(), request)
        );
        assertEquals("EXPIRED", pendingApproval.getStatus());
        verify(containerService, never()).executeContainerStateChange(any(), any());
    }
}
