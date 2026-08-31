package ai.devops.modules.infrastructure.container;

import ai.devops.common.exception.ApprovalRequiredException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private ContainerService containerService;

    private ContainerEntity prodContainer;
    private ContainerEntity devContainer;
    private EnvironmentEntity prodEnv;
    private EnvironmentEntity devEnv;

    @BeforeEach
    void setUp() {
        containerService = new ContainerService(containerRepository, approvalService, auditService, userRepository);

        prodEnv = new EnvironmentEntity(null, "PRODUCTION", "Production env", true);
        devEnv = new EnvironmentEntity(null, "DEVELOPMENT", "Development env", false);

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
    }

    @Test
    @DisplayName("Production container mutation must throw ApprovalRequiredException when unapproved")
    void testProductionActionRequiresApproval() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("devops@devops.ai", "pass", List.of(new SimpleGrantedAuthority("ROLE_DEVOPS_ENGINEER")))
        );

        when(containerRepository.findById(prodContainer.getId())).thenReturn(Optional.of(prodContainer));

        ApprovalRequestEntity mockApproval = new ApprovalRequestEntity();
        mockApproval.setId(UUID.randomUUID());
        when(approvalService.createApprovalRequest(any(), any(), any(), any(), any(), any())).thenReturn(mockApproval);

        ApprovalRequiredException ex = assertThrows(ApprovalRequiredException.class, () ->
                containerService.executeContainerAction(prodContainer.getId(), "restart", false)
        );

        assertEquals("APPROVAL_REQUIRED", ex.getCode());
        verify(approvalService, times(1)).createApprovalRequest(any(), eq(prodEnv), any(), eq("restart_container"), any(), any());
    }

    @Test
    @DisplayName("Development container mutation executes immediately without human approval")
    void testDevActionExecutesImmediately() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("devops@devops.ai", "pass", List.of(new SimpleGrantedAuthority("ROLE_DEVOPS_ENGINEER")))
        );

        when(containerRepository.findById(devContainer.getId())).thenReturn(Optional.of(devContainer));

        Map<String, Object> result = containerService.executeContainerAction(devContainer.getId(), "restart", false);

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("RUNNING", result.get("newState"));
        verify(containerRepository, times(1)).save(devContainer);
    }
}
