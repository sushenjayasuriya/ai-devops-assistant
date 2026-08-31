package ai.devops.modules.overview;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.deployment.repository.DeploymentRepository;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.repository.IncidentRepository;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/overview")
@Tag(name = "Overview", description = "High-level aggregate SRE dashboard metrics, health summaries, and system topology")
public class OverviewController {

    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final ContainerRepository containerRepository;
    private final IncidentRepository incidentRepository;
    private final DeploymentRepository deploymentRepository;

    public OverviewController(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            ContainerRepository containerRepository,
            IncidentRepository incidentRepository,
            DeploymentRepository deploymentRepository) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.containerRepository = containerRepository;
        this.incidentRepository = incidentRepository;
        this.deploymentRepository = deploymentRepository;
    }

    @GetMapping
    @Operation(summary = "Get global infrastructure overview and health summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(@RequestParam(required = false) UUID envId) {
        long serverCount = envId != null ? serverRepository.findByEnvironmentId(envId).size() : serverRepository.count();
        long containerCount = envId != null ? containerRepository.findByEnvironmentId(envId).size() : containerRepository.count();
        long openIncidents = envId != null ?
                incidentRepository.findByEnvironmentIdAndStatusOrderByStartedAtDesc(envId, IncidentStatus.OPEN).size() :
                incidentRepository.findByStatusOrderByStartedAtDesc(IncidentStatus.OPEN).size();
        long recentDeployments = envId != null ?
                deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(envId).size() :
                deploymentRepository.count();

        String healthSummary = openIncidents > 0 ? "DEGRADED" : "HEALTHY";

        Map<String, Object> summary = new HashMap<>();
        summary.put("healthSummary", healthSummary);
        summary.put("serverCount", serverCount);
        summary.put("containerCount", containerCount);
        summary.put("openIncidentsCount", openIncidents);
        summary.put("recentDeploymentsCount", recentDeployments);
        summary.put("averageCpuPercent", "DEGRADED".equals(healthSummary) ? 68.4 : 24.1);
        summary.put("averageMemoryPercent", "DEGRADED".equals(healthSummary) ? 76.8 : 42.0);
        summary.put("averageDiskPercent", 62.5);
        summary.put("activeEnvironments", environmentRepository.count());

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
