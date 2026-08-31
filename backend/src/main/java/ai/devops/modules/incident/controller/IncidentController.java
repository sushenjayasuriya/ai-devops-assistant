package ai.devops.modules.incident.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.entity.IncidentEventEntity;
import ai.devops.modules.incident.service.IncidentEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Endpoints for managing real-time infrastructure incidents, anomaly timelines, and root-cause analysis")
public class IncidentController {

    private final IncidentEngineService incidentEngineService;

    public IncidentController(IncidentEngineService incidentEngineService) {
        this.incidentEngineService = incidentEngineService;
    }

    @GetMapping
    @Operation(summary = "List incidents with optional environment and status filters")
    public ResponseEntity<ApiResponse<List<IncidentEntity>>> getIncidents(
            @RequestParam(required = false) UUID envId,
            @RequestParam(required = false) IncidentStatus status) {
        List<IncidentEntity> incidents = incidentEngineService.getIncidents(envId, status);
        return ResponseEntity.ok(ApiResponse.ok(incidents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details by ID")
    public ResponseEntity<ApiResponse<IncidentEntity>> getIncidentById(@PathVariable UUID id) {
        IncidentEntity incident = incidentEngineService.getIncidentById(id);
        return ResponseEntity.ok(ApiResponse.ok(incident));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get chronological event stream for an incident")
    public ResponseEntity<ApiResponse<List<IncidentEventEntity>>> getIncidentEvents(@PathVariable UUID id) {
        List<IncidentEventEntity> events = incidentEngineService.getIncidentEvents(id);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update incident lifecycle status (e.g. INVESTIGATING, MITIGATED, RESOLVED)")
    public ResponseEntity<ApiResponse<IncidentEntity>> updateStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status) {
        IncidentEntity updated = incidentEngineService.updateIncidentStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", updated));
    }

    @GetMapping("/{id}/investigation")
    @Operation(summary = "Get AI and correlation engine analysis for an incident")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvestigation(@PathVariable UUID id) {
        Map<String, Object> investigation = incidentEngineService.getIncidentInvestigation(id);
        return ResponseEntity.ok(ApiResponse.ok(investigation));
    }
}
