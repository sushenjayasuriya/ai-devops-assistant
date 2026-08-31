package ai.devops.modules.ai.repository;

import ai.devops.modules.ai.entity.AIConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversationEntity, UUID> {
    @Query("SELECT c FROM AIConversationEntity c WHERE c.user.organization.id = :orgId ORDER BY c.updatedAt DESC")
    List<AIConversationEntity> findByOrganizationIdOrderByUpdatedAtDesc(@Param("orgId") UUID orgId);

    @Query("SELECT c FROM AIConversationEntity c WHERE c.id = :id AND c.user.organization.id = :orgId")
    Optional<AIConversationEntity> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

    List<AIConversationEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<AIConversationEntity> findByEnvironmentIdOrderByUpdatedAtDesc(UUID environmentId);
}
