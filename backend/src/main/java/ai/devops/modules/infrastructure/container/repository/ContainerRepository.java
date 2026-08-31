package ai.devops.modules.infrastructure.container.repository;

import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerEntity, UUID> {
    @Query("SELECT c FROM ContainerEntity c WHERE c.environment.organization.id = :orgId")
    List<ContainerEntity> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT c FROM ContainerEntity c WHERE c.environment.organization.id = :orgId AND c.environment.id = :envId")
    List<ContainerEntity> findByOrganizationIdAndEnvironmentId(@Param("orgId") UUID orgId, @Param("envId") UUID envId);

    @Query("SELECT c FROM ContainerEntity c WHERE c.id = :id AND c.environment.organization.id = :orgId")
    Optional<ContainerEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    @Query("SELECT c FROM ContainerEntity c WHERE c.name = :name AND c.environment.organization.id = :orgId")
    Optional<ContainerEntity> findByNameAndOrganizationId(@Param("name") String name, @Param("orgId") UUID orgId);

    List<ContainerEntity> findByEnvironmentId(UUID environmentId);
    Optional<ContainerEntity> findByContainerId(String containerId);
    Optional<ContainerEntity> findByName(String name);
}
