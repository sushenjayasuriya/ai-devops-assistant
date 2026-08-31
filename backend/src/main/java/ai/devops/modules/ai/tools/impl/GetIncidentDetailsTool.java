package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.service.IncidentEngineService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetIncidentDetailsTool implements DevOpsTool {

    private final IncidentEngineService incidentEngineService;

    public GetIncidentDetailsTool(IncidentEngineService incidentEngineService) {
        this.incidentEngineService = incidentEngineService;
    }

    @Override
    public String getName() {
        return "get_incident_details";
    }

    @Override
    public String getDescription() {
        return "Retrieve active alerts, incident status, root-cause timelines, and impacted services.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of(
                "incidentId", "string (optional): Incident UUID to retrieve specific incident analysis",
                "status", "string (optional): Filter status (OPEN, INVESTIGATING, MITIGATED, RESOLVED, CLOSED)"
        );
    }

    @Override
    public Set<String> getRequiredParameters() {
        return Set.of();
    }

    @Override
    public Set<String> getAllowedParameters() {
        return Set.of("incidentId", "status");
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.READ_ONLY;
    }

    @Override
    public Role getRequiredRole() {
        return Role.VIEWER;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean requiresProductionApproval() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        if (parameters.containsKey("incidentId") && parameters.get("incidentId") != null && !String.valueOf(parameters.get("incidentId")).isBlank()) {
            UUID id = UUID.fromString(String.valueOf(parameters.get("incidentId")));
            Map<String, Object> investigation = incidentEngineService.getIncidentInvestigation(id);
            return ToolExecutionResult.ok(getName(), investigation);
        }

        IncidentStatus status = null;
        if (parameters.containsKey("status") && parameters.get("status") != null && !String.valueOf(parameters.get("status")).isBlank()) {
            try {
                status = IncidentStatus.valueOf(String.valueOf(parameters.get("status")).toUpperCase());
            } catch (Exception ignored) {}
        }

        List<IncidentEntity> incidents = incidentEngineService.getIncidents(null, status);
        List<Map<String, Object>> summary = incidents.stream()
                .map(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", i.getId());
                    map.put("title", i.getTitle());
                    map.put("severity", i.getSeverity());
                    map.put("status", i.getStatus());
                    map.put("affectedResourceType", i.getAffectedResourceType());
                    map.put("affectedResourceId", i.getAffectedResourceId());
                    map.put("environment", i.getEnvironment() != null ? i.getEnvironment().getName() : "UNKNOWN");
                    map.put("startedAt", i.getStartedAt());
                    return map;
                })
                .toList();

        return ToolExecutionResult.ok(getName(), summary);
    }
}
