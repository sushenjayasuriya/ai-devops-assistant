package ai.devops.modules.deployment.repository;

import ai.devops.modules.deployment.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, UUID> {
    @Query("SELECT d FROM DeploymentEntity d WHERE d.environment.organization.id = :orgId ORDER BY d.startedAt DESC")
    List<DeploymentEntity> findByOrganizationIdOrderByStartedAtDesc(@Param("orgId") UUID orgId);

    @Query("SELECT d FROM DeploymentEntity d WHERE d.environment.organization.id = :orgId AND d.environment.id = :envId ORDER BY d.startedAt DESC")
    List<DeploymentEntity> findByOrganizationIdAndEnvironmentIdOrderByStartedAtDesc(@Param("orgId") UUID orgId, @Param("envId") UUID envId);

    @Query("SELECT d FROM DeploymentEntity d WHERE d.id = :id AND d.environment.organization.id = :orgId")
    Optional<DeploymentEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    List<DeploymentEntity> findByEnvironmentIdOrderByStartedAtDesc(UUID environmentId);
    List<DeploymentEntity> findByServiceNameOrderByStartedAtDesc(String serviceName);
    List<DeploymentEntity> findAllByOrderByStartedAtDesc();
}
