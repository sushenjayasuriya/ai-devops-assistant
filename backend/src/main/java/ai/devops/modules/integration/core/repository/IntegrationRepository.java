package ai.devops.modules.integration.core.repository;

import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID> {
    List<IntegrationEntity> findByEnvironmentId(UUID environmentId);
    Optional<IntegrationEntity> findByEnvironmentIdAndType(UUID environmentId, IntegrationType type);
}
