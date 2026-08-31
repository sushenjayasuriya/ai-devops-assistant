package ai.devops.modules.integration.core.repository;

import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID> {
    @Query("SELECT i FROM IntegrationEntity i WHERE i.environment.organization.id = :orgId")
    List<IntegrationEntity> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT i FROM IntegrationEntity i WHERE i.environment.organization.id = :orgId AND i.environment.id = :envId")
    List<IntegrationEntity> findByOrganizationIdAndEnvironmentId(@Param("orgId") UUID orgId, @Param("envId") UUID envId);

    @Query("SELECT i FROM IntegrationEntity i WHERE i.id = :id AND i.environment.organization.id = :orgId")
    Optional<IntegrationEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    List<IntegrationEntity> findByEnvironmentId(UUID environmentId);
    List<IntegrationEntity> findByType(IntegrationType type);
}
