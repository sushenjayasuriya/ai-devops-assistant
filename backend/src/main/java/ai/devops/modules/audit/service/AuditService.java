package ai.devops.modules.audit.service;

import ai.devops.common.logging.CorrelationIdFilter;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.entity.AuditLogEntity;
import ai.devops.modules.audit.repository.AuditLogRepository;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.OrganizationRepository;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(password|secret|token|key|authorization|bearer)\\s*[:=]\\s*['\"]?([^'\",\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        entry.setSanitizedParameters(maskSensitiveData(sanitizedParams));
        entry.setStatus(status);
        entry.setErrorDetails(maskSensitiveData(errorDetails));
        entry.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        entry.setTimestamp(Instant.now());

        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        if (currentOrgId != null) {
            organizationRepository.findById(currentOrgId).ifPresent(entry::setOrganization);
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!"system".equalsIgnoreCase(currentUserEmail)) {
            Optional<UserEntity> user = userRepository.findByEmail(currentUserEmail);
            user.ifPresent(u -> {
                entry.setUser(u);
                if (entry.getOrganization() == null && u.getOrganization() != null) {
                    entry.setOrganization(u.getOrganization());
                }
            });
        }

        AuditLogEntity saved = auditLogRepository.save(entry);
        log.info("[AUDIT] org={} id={} action={} target={}:{} env={} risk={} status={} user={}",
                entry.getOrganization() != null ? entry.getOrganization().getSlug() : "GLOBAL",
                saved.getId(), action, targetResourceType, targetResourceId, environmentName, riskLevel, status, currentUserEmail);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogs(String envName, Pageable pageable) {
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        if (currentOrgId == null) {
            return Page.empty(pageable);
        }

        if (envName != null && !envName.isBlank()) {
            return auditLogRepository.findByOrganizationIdAndEnvironmentNameOrderByTimestampDesc(
                    currentOrgId, envName.toUpperCase(), pageable);
        }
        return auditLogRepository.findByOrganizationIdOrderByTimestampDesc(currentOrgId, pageable);
    }

    public static String maskSensitiveData(String input) {
        if (input == null || input.isBlank()) return input;
        return SENSITIVE_PATTERN.matcher(input).replaceAll("$1=********");
    }
}
