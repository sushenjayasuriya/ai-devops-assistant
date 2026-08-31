package ai.devops.modules.infrastructure.container.repository;

import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerEntity, UUID> {
    List<ContainerEntity> findByEnvironmentId(UUID environmentId);
    Optional<ContainerEntity> findByContainerId(String containerId);
    Optional<ContainerEntity> findByName(String name);
}
