package ai.devops.modules.infrastructure.container;

import ai.devops.common.exception.ApprovalRequiredException;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContainerApprovalTest {

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private ApprovalWorkflowService approvalService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DockerIntegration dockerIntegration;

    @Mock
    private IntegrationRepository integrationRepository;

    private ContainerService containerService;

    private OrganizationEntity org;
    private UserEntity user;
    private ContainerEntity prodContainer;
    private ContainerEntity devContainer;
    private EnvironmentEntity prodEnv;
    private EnvironmentEntity devEnv;

    @BeforeEach
    void setUp() {
        containerService = new ContainerService(
                containerRepository,
                approvalService,
                auditService,
                userRepository,
                dockerIntegration,
                integrationRepository,
                "tcp://localhost:2375"
        );

        org = new OrganizationEntity("Acme Corp", "acme-corp");
        org.setId(UUID.randomUUID());

        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("devops@devops.ai");
        user.setOrganization(org);
        user.setRoles(Set.of("DEVOPS_ENGINEER"));
        user.setEnabled(true);

        prodEnv = new EnvironmentEntity(org, "PRODUCTION", "Production env", true);
        prodEnv.setId(UUID.randomUUID());

        devEnv = new EnvironmentEntity(org, "DEVELOPMENT", "Development env", false);
        devEnv.setId(UUID.randomUUID());

        prodContainer = new ContainerEntity();
        prodContainer.setId(UUID.randomUUID());
        prodContainer.setName("thingsboard-core-app");
        prodContainer.setContainerId("d9f8e7a6b5c4");
        prodContainer.setEnvironment(prodEnv);
        prodContainer.setState("RESTARTING");

        devContainer = new ContainerEntity();
        devContainer.setId(UUID.randomUUID());
        devContainer.setName("dev-app");
        devContainer.setContainerId("c1c2c3c4");
        devContainer.setEnvironment(devEnv);
        devContainer.setState("RUNNING");

        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    @DisplayName("Production container mutation must strictly throw ApprovalRequiredException and create approval request")
    void testProductionActionRequiresApproval() {
        when(containerRepository.findByIdAndOrganizationId(prodContainer.getId(), org.getId()))
                .thenReturn(Optional.of(prodContainer));

        ApprovalRequestEntity mockApproval = new ApprovalRequestEntity();
        mockApproval.setId(UUID.randomUUID());
        when(approvalService.createApprovalRequest(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockApproval);

        ApprovalRequiredException ex = assertThrows(ApprovalRequiredException.class, () ->
                containerService.requestOrExecuteContainerAction(prodContainer.getId(), "restart")
        );

        assertEquals("APPROVAL_REQUIRED", ex.getCode());
        verify(approvalService, times(1)).createApprovalRequest(
                isNull(), eq(prodEnv), any(), eq("restart_container"), eq("CONTAINER"),
                eq(prodContainer.getId().toString()), eq(prodContainer.getName()), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("Development container mutation executes immediately without human approval")
    void testDevActionExecutesImmediately() {
        when(containerRepository.findByIdAndOrganizationId(devContainer.getId(), org.getId()))
                .thenReturn(Optional.of(devContainer));

        Map<String, Object> result = containerService.requestOrExecuteContainerAction(devContainer.getId(), "restart");

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("RUNNING", result.get("newState"));
        verify(containerRepository, times(1)).save(devContainer);
    }
}
