package ai.devops.modules.infrastructure.server.repository;

import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<ServerEntity, UUID> {
    @Query("SELECT s FROM ServerEntity s WHERE s.environment.organization.id = :orgId")
    List<ServerEntity> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT s FROM ServerEntity s WHERE s.environment.organization.id = :orgId AND s.environment.id = :envId")
    List<ServerEntity> findByOrganizationIdAndEnvironmentId(@Param("orgId") UUID orgId, @Param("envId") UUID envId);

    @Query("SELECT s FROM ServerEntity s WHERE s.id = :id AND s.environment.organization.id = :orgId")
    Optional<ServerEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    List<ServerEntity> findByEnvironmentId(UUID environmentId);
    Optional<ServerEntity> findByHostname(String hostname);
}
