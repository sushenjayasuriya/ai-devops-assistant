package ai.devops.modules.ai.repository;

import ai.devops.modules.ai.entity.AIMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessageEntity, UUID> {
    List<AIMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
