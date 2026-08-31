package ai.devops.modules.ai.tools.impl;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.repository.IncidentRepository;
import ai.devops.modules.incident.service.IncidentEngineService;
import ai.devops.security.rbac.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class GetIncidentDetailsTool implements DevOpsTool {

    private final IncidentRepository incidentRepository;
    private final IncidentEngineService incidentService;

    public GetIncidentDetailsTool(IncidentRepository incidentRepository, IncidentEngineService incidentService) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
    }

    @Override
    public String getName() {
        return "get_incident_details";
    }

    @Override
    public String getDescription() {
        return "Retrieve the current active incidents, severity, affected resources, and timeline events.";
    }

    @Override
    public Map<String, String> getParameterSchema() {
        return Map.of("incidentId", "string (optional): Incident UUID");
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
    public boolean requiresProductionApproval() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        String incidentIdStr = parameters != null && parameters.containsKey("incidentId") ?
                String.valueOf(parameters.get("incidentId")) : null;

        if (incidentIdStr != null && !incidentIdStr.isBlank()) {
            try {
                UUID id = UUID.fromString(incidentIdStr);
                Optional<IncidentEntity> incidentOpt = incidentRepository.findById(id);
                if (incidentOpt.isPresent()) {
                    IncidentEntity inc = incidentOpt.get();
                    Map<String, Object> details = Map.of(
                            "id", inc.getId(),
                            "title", inc.getTitle(),
                            "severity", inc.getSeverity().name(),
                            "status", inc.getStatus().name(),
                            "resource", inc.getAffectedResourceId() != null ? inc.getAffectedResourceId() : "",
                            "startedAt", inc.getStartedAt(),
                            "events", incidentService.getIncidentEvents(inc.getId())
                    );
                    return ToolExecutionResult.ok(getName(), details);
                }
            } catch (Exception ignored) {}
        }

        List<IncidentEntity> incidents = incidentRepository.findAllByOrderByStartedAtDesc();
        List<Map<String, Object>> result = incidents.stream()
                .map(inc -> Map.<String, Object>of(
                        "id", inc.getId(),
                        "title", inc.getTitle(),
                        "severity", inc.getSeverity().name(),
                        "status", inc.getStatus().name(),
                        "resource", inc.getAffectedResourceId() != null ? inc.getAffectedResourceId() : "",
                        "startedAt", inc.getStartedAt()
                ))
                .toList();

        return ToolExecutionResult.ok(getName(), result);
    }
}
