package ai.devops.modules.incident.repository;

import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
    List<IncidentEntity> findByEnvironmentIdOrderByStartedAtDesc(UUID environmentId);
    List<IncidentEntity> findByStatusOrderByStartedAtDesc(IncidentStatus status);
    List<IncidentEntity> findByEnvironmentIdAndStatusOrderByStartedAtDesc(UUID environmentId, IncidentStatus status);
    List<IncidentEntity> findAllByOrderByStartedAtDesc();
}
