package ai.devops.modules.incident.repository;

import ai.devops.modules.incident.entity.IncidentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, UUID> {
    List<IncidentEventEntity> findByIncidentIdOrderByTimestampAsc(UUID incidentId);
}
