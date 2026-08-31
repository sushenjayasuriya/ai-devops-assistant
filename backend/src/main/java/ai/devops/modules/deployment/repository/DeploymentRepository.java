package ai.devops.modules.deployment.repository;

import ai.devops.modules.deployment.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, UUID> {
    List<DeploymentEntity> findByEnvironmentIdOrderByStartedAtDesc(UUID environmentId);
    List<DeploymentEntity> findAllByOrderByStartedAtDesc();
    List<DeploymentEntity> findByServiceNameOrderByStartedAtDesc(String serviceName);
}
