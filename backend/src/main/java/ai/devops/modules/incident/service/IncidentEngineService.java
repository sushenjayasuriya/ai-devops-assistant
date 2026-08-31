package ai.devops.modules.incident.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.incident.IncidentSeverity;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.entity.IncidentEventEntity;
import ai.devops.modules.incident.repository.IncidentEventRepository;
import ai.devops.modules.incident.repository.IncidentRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IncidentEngineService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository eventRepository;
    private final IncidentCorrelationService correlationService;

    public IncidentEngineService(
            IncidentRepository incidentRepository,
            IncidentEventRepository eventRepository,
            IncidentCorrelationService correlationService) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.correlationService = correlationService;
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> getIncidents(UUID envId, IncidentStatus status) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }

        if (envId != null && status != null) {
            return incidentRepository.findByOrganizationIdAndEnvironmentIdAndStatusOrderByStartedAtDesc(orgId, envId, status);
        } else if (envId != null) {
            return incidentRepository.findByOrganizationIdAndEnvironmentIdOrderByStartedAtDesc(orgId, envId);
        } else if (status != null) {
            return incidentRepository.findByOrganizationIdAndStatusOrderByStartedAtDesc(orgId, status);
        }
        return incidentRepository.findByOrganizationIdOrderByStartedAtDesc(orgId);
    }

    @Transactional(readOnly = true)
    public IncidentEntity getIncidentById(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ResourceNotFoundException("Incident", id);
        }

        return incidentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    @Transactional(readOnly = true)
    public List<IncidentEventEntity> getIncidentEvents(UUID incidentId) {
        IncidentEntity incident = getIncidentById(incidentId); // Validates organization ownership!
        return eventRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());
    }

    @Transactional
    public IncidentEntity updateIncidentStatus(UUID id, IncidentStatus newStatus) {
        IncidentEntity incident = getIncidentById(id); // Validates organization ownership!
        incident.setStatus(newStatus);
        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            incident.setResolvedAt(Instant.now());
        }

        IncidentEventEntity event = new IncidentEventEntity(
                incident,
                "STATE_CHANGE",
                String.format("Incident status updated to %s", newStatus),
                String.format("{\"newStatus\": \"%s\"}", newStatus)
        );
        eventRepository.save(event);

        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getIncidentInvestigation(UUID id) {
        IncidentEntity incident = getIncidentById(id); // Validates organization ownership!
        return correlationService.correlateIncident(incident);
    }
}
