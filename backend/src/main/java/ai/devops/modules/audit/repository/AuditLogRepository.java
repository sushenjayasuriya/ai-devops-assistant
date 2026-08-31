package ai.devops.modules.audit.repository;

import ai.devops.modules.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    Page<AuditLogEntity> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLogEntity> findByEnvironmentNameOrderByTimestampDesc(String environmentName, Pageable pageable);
    Page<AuditLogEntity> findByCorrelationIdOrderByTimestampDesc(String correlationId, Pageable pageable);
}
