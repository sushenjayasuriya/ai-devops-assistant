package ai.devops.modules.ai.repository;

import ai.devops.modules.ai.entity.AIActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIActionRepository extends JpaRepository<AIActionEntity, UUID> {
    List<AIActionEntity> findByMessageId(UUID messageId);
}
