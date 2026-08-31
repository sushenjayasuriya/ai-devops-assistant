package ai.devops.security.tenant;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.modules.infrastructure.server.service.ServerService;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.modules.integration.linux.LinuxServerIntegration;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.audit.service.AuditService;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private ApprovalWorkflowService approvalService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LinuxServerIntegration linuxIntegration;

    @Mock
    private DockerIntegration dockerIntegration;

    @Mock
    private IntegrationRepository integrationRepository;

    private ServerService serverService;
    private ContainerService containerService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private EnvironmentEntity envA;
    private EnvironmentEntity envB;
    private ServerEntity serverA;
    private ServerEntity serverB;
    private ContainerEntity containerA;
    private ContainerEntity containerB;

    @BeforeEach
    void setUp() {
        serverService = new ServerService(serverRepository, linuxIntegration);
        containerService = new ContainerService(
                containerRepository,
                approvalService,
                auditService,
                userRepository,
                dockerIntegration,
                integrationRepository,
                "tcp://localhost:2375"
        );

        orgA = new OrganizationEntity("Tenant A", "tenant-a");
        orgA.setId(UUID.randomUUID());

        orgB = new OrganizationEntity("Tenant B", "tenant-b");
        orgB.setId(UUID.randomUUID());

        userA = new UserEntity();
        userA.setId(UUID.randomUUID());
        userA.setEmail("operator@tenant-a.com");
        userA.setOrganization(orgA);
        userA.setRoles(Set.of("DEVOPS_ENGINEER"));
        userA.setEnabled(true);

        envA = new EnvironmentEntity(orgA, "PRODUCTION", "Tenant A Prod", true);
        envA.setId(UUID.randomUUID());

        envB = new EnvironmentEntity(orgB, "PRODUCTION", "Tenant B Prod", true);
        envB.setId(UUID.randomUUID());

        serverA = new ServerEntity();
        serverA.setId(UUID.randomUUID());
        serverA.setEnvironment(envA);
        serverA.setHostname("server-a.internal");
        serverA.setIpAddress("10.0.1.10");
        serverA.setStatus("ONLINE");

        serverB = new ServerEntity();
        serverB.setId(UUID.randomUUID());
        serverB.setEnvironment(envB);
        serverB.setHostname("server-b.internal");
        serverB.setIpAddress("10.0.2.20");
        serverB.setStatus("ONLINE");

        containerA = new ContainerEntity();
        containerA.setId(UUID.randomUUID());
        containerA.setEnvironment(envA);
        containerA.setServer(serverA);
        containerA.setName("app-a");
        containerA.setContainerId("c-a-1");
        containerA.setImage("app-a:latest");
        containerA.setState("RUNNING");

        containerB = new ContainerEntity();
        containerB.setId(UUID.randomUUID());
        containerB.setEnvironment(envB);
        containerB.setServer(serverB);
        containerB.setName("app-b");
        containerB.setContainerId("c-b-1");
        containerB.setImage("app-b:latest");
        containerB.setState("RUNNING");

        // Set authenticated user to UserA (Tenant A)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Tenant A can read its own servers")
    void testTenantCanReadOwnServer() {
        when(serverRepository.findByIdAndOrganizationId(serverA.getId(), orgA.getId()))
                .thenReturn(Optional.of(serverA));

        ServerEntity result = serverService.getServerById(serverA.getId());
        assertNotNull(result);
        assertEquals(serverA.getId(), result.getId());
    }

    @Test
    @DisplayName("Tenant A is denied access when attempting to read Tenant B's server by UUID")
    void testTenantCannotReadOtherTenantServer() {
        when(serverRepository.findByIdAndOrganizationId(serverB.getId(), orgA.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serverService.getServerById(serverB.getId()));
    }

    @Test
    @DisplayName("Tenant A is denied access when attempting to mutate Tenant B's container")
    void testTenantCannotMutateOtherTenantContainer() {
        when(containerRepository.findByIdAndOrganizationId(containerB.getId(), orgA.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> containerService.requestOrExecuteContainerAction(containerB.getId(), "restart"));
    }

    @Test
    @DisplayName("Tenant A listing containers only receives Tenant A containers")
    void testTenantContainerListingIsFiltered() {
        when(containerRepository.findByOrganizationId(orgA.getId()))
                .thenReturn(List.of(containerA));

        List<ContainerEntity> containers = containerService.getContainers(null);
        assertEquals(1, containers.size());
        assertEquals("app-a", containers.get(0).getName());
        verify(containerRepository, times(1)).findByOrganizationId(orgA.getId());
        verify(containerRepository, never()).findAll();
    }
}
