package ai.devops.modules.incident.repository;

import ai.devops.modules.incident.IncidentStatus;
import ai.devops.modules.incident.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {
    @Query("SELECT i FROM IncidentEntity i WHERE i.environment.organization.id = :orgId ORDER BY i.startedAt DESC")
    List<IncidentEntity> findByOrganizationIdOrderByStartedAtDesc(@Param("orgId") UUID orgId);

    @Query("SELECT i FROM IncidentEntity i WHERE i.environment.organization.id = :orgId AND i.status = :status ORDER BY i.startedAt DESC")
    List<IncidentEntity> findByOrganizationIdAndStatusOrderByStartedAtDesc(@Param("orgId") UUID orgId, @Param("status") IncidentStatus status);

    @Query("SELECT i FROM IncidentEntity i WHERE i.environment.organization.id = :orgId AND i.environment.id = :envId ORDER BY i.startedAt DESC")
    List<IncidentEntity> findByOrganizationIdAndEnvironmentIdOrderByStartedAtDesc(@Param("orgId") UUID orgId, @Param("envId") UUID envId);

    @Query("SELECT i FROM IncidentEntity i WHERE i.environment.organization.id = :orgId AND i.environment.id = :envId AND i.status = :status ORDER BY i.startedAt DESC")
    List<IncidentEntity> findByOrganizationIdAndEnvironmentIdAndStatusOrderByStartedAtDesc(@Param("orgId") UUID orgId, @Param("envId") UUID envId, @Param("status") IncidentStatus status);

    @Query("SELECT i FROM IncidentEntity i WHERE i.id = :id AND i.environment.organization.id = :orgId")
    Optional<IncidentEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    List<IncidentEntity> findByEnvironmentIdOrderByStartedAtDesc(UUID environmentId);
    List<IncidentEntity> findByStatusOrderByStartedAtDesc(IncidentStatus status);
    List<IncidentEntity> findByEnvironmentIdAndStatusOrderByStartedAtDesc(UUID environmentId, IncidentStatus status);
    List<IncidentEntity> findAllByOrderByStartedAtDesc();
}
