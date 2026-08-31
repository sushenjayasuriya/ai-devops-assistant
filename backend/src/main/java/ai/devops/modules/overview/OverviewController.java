package ai.devops.modules.overview;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.deployment.repository.DeploymentRepository;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.repository.IncidentRepository;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.infrastructure.server.repository.ServerRepository;
import ai.devops.security.rbac.SecurityUtils;
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
    @Operation(summary = "Get global infrastructure overview and health summary for current tenant organization")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(@RequestParam(required = false) UUID envId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("healthSummary", "HEALTHY", "serverCount", 0, "containerCount", 0)));
        }

        long serverCount = envId != null ?
                serverRepository.findByOrganizationIdAndEnvironmentId(orgId, envId).size() :
                serverRepository.findByOrganizationId(orgId).size();

        long containerCount = envId != null ?
                containerRepository.findByOrganizationIdAndEnvironmentId(orgId, envId).size() :
                containerRepository.findByOrganizationId(orgId).size();

        long openIncidents = envId != null ?
                incidentRepository.findByOrganizationIdAndEnvironmentIdAndStatusOrderByStartedAtDesc(orgId, envId, IncidentStatus.OPEN).size() :
                incidentRepository.findByOrganizationIdAndStatusOrderByStartedAtDesc(orgId, IncidentStatus.OPEN).size();

        long recentDeployments = envId != null ?
                deploymentRepository.findByOrganizationIdAndEnvironmentIdOrderByStartedAtDesc(orgId, envId).size() :
                deploymentRepository.findByOrganizationIdOrderByStartedAtDesc(orgId).size();

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
        summary.put("activeEnvironments", environmentRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).size());

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
