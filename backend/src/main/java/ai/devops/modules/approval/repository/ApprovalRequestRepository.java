package ai.devops.modules.approval.repository;

import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    List<ApprovalRequestEntity> findByStatusOrderByRequestedAtDesc(String status);
    List<ApprovalRequestEntity> findByEnvironmentIdOrderByRequestedAtDesc(UUID environmentId);
}
