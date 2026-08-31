package ai.devops.modules.incident;

import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.repository.DeploymentRepository;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.service.IncidentCorrelationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentCorrelationTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    private IncidentCorrelationService correlationService;

    private EnvironmentEntity prodEnv;
    private IncidentEntity mockIncident;
    private DeploymentEntity mockDeployment;

    @BeforeEach
    void setUp() {
        correlationService = new IncidentCorrelationService(deploymentRepository);

        prodEnv = new EnvironmentEntity(null, "PRODUCTION", "Production", true);
        prodEnv.setId(UUID.randomUUID());

        mockIncident = new IncidentEntity();
        mockIncident.setId(UUID.randomUUID());
        mockIncident.setTitle("ThingsBoard API High Latency");
        mockIncident.setAffectedResourceId("thingsboard-core-app");
        mockIncident.setEnvironment(prodEnv);
        mockIncident.setStartedAt(Instant.now());

        mockDeployment = new DeploymentEntity();
        mockDeployment.setId(UUID.randomUUID());
        mockDeployment.setServiceName("thingsboard-core-app");
        mockDeployment.setVersionTag("v3.6.2-patch184");
        mockDeployment.setCommitSha("a9b8c7d");
        mockDeployment.setStartedAt(Instant.now().minusSeconds(900)); // 15 mins ago
    }

    @Test
    @DisplayName("Should correlate incident with recent deployment and calculate high confidence root cause")
    void testIncidentCorrelation() {
        when(deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(prodEnv.getId()))
                .thenReturn(List.of(mockDeployment));

        Map<String, Object> correlation = correlationService.correlateIncident(mockIncident);

        assertNotNull(correlation);
        assertEquals(0.91, (Double) correlation.get("confidence"), 0.01);
        assertEquals("restart_container", correlation.get("recommendedAction"));
        assertTrue(correlation.get("probableRootCause").toString().contains("v3.6.2-patch184"));
    }
}
