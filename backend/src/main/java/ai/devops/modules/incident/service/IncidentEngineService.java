package ai.devops.modules.incident.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.incident.IncidentSeverity;
import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.incident.entity.IncidentEventEntity;
import ai.devops.modules.incident.repository.IncidentEventRepository;
import ai.devops.modules.incident.repository.IncidentRepository;
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
        if (envId != null && status != null) {
            return incidentRepository.findByEnvironmentIdAndStatusOrderByStartedAtDesc(envId, status);
        } else if (envId != null) {
            return incidentRepository.findByEnvironmentIdOrderByStartedAtDesc(envId);
        } else if (status != null) {
            return incidentRepository.findByStatusOrderByStartedAtDesc(status);
        }
        return incidentRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public IncidentEntity getIncidentById(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    @Transactional(readOnly = true)
    public List<IncidentEventEntity> getIncidentEvents(UUID incidentId) {
        return eventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
    }

    @Transactional
    public IncidentEntity updateIncidentStatus(UUID id, IncidentStatus newStatus) {
        IncidentEntity incident = getIncidentById(id);
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
        IncidentEntity incident = getIncidentById(id);
        return correlationService.correlateIncident(incident);
    }
}
