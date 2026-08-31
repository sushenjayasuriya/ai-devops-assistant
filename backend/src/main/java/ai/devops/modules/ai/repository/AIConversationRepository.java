package ai.devops.modules.ai.repository;

import ai.devops.modules.ai.entity.AIConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversationEntity, UUID> {
    List<AIConversationEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<AIConversationEntity> findByEnvironmentIdOrderByUpdatedAtDesc(UUID environmentId);
}
