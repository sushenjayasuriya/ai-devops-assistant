package ai.devops.modules.environment.repository;

import ai.devops.modules.environment.entity.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {
    List<EnvironmentEntity> findAllByOrderByCreatedAtAsc();
    Optional<EnvironmentEntity> findByNameIgnoreCase(String name);
}
