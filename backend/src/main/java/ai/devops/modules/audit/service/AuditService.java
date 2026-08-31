package ai.devops.modules.audit.service;

import ai.devops.common.logging.CorrelationIdFilter;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.entity.AuditLogEntity;
import ai.devops.modules.audit.repository.AuditLogRepository;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuditLogEntity recordAudit(
            String action,
            String targetResourceType,
            String targetResourceId,
            String environmentName,
            RiskLevel riskLevel,
            String sanitizedParams,
            String status,
            String errorDetails,
            String ipAddress) {

        AuditLogEntity entry = new AuditLogEntity();
        entry.setCorrelationId(CorrelationIdFilter.getCurrentCorrelationId());
        entry.setAction(action);
        entry.setTargetResourceType(targetResourceType);
        entry.setTargetResourceId(targetResourceId);
        entry.setEnvironmentName(environmentName);
        entry.setRiskLevel(riskLevel != null ? riskLevel.name() : RiskLevel.READ_ONLY.name());
        entry.setSanitizedParameters(sanitizedParams);
        entry.setStatus(status);
        entry.setErrorDetails(errorDetails);
        entry.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        entry.setTimestamp(Instant.now());

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!"system".equalsIgnoreCase(currentUserEmail)) {
            Optional<UserEntity> user = userRepository.findByEmail(currentUserEmail);
            user.ifPresent(entry::setUser);
        }

        AuditLogEntity saved = auditLogRepository.save(entry);
        log.info("[AUDIT] id={} action={} target={}:{} env={} risk={} status={} user={}",
                saved.getId(), action, targetResourceType, targetResourceId, environmentName, riskLevel, status, currentUserEmail);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogs(String envName, Pageable pageable) {
        if (envName != null && !envName.isBlank()) {
            return auditLogRepository.findByEnvironmentNameOrderByTimestampDesc(envName.toUpperCase(), pageable);
        }
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
