package ai.devops.modules.infrastructure.server.repository;

import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<ServerEntity, UUID> {
    List<ServerEntity> findByEnvironmentId(UUID environmentId);
    Optional<ServerEntity> findByHostname(String hostname);
}
