package ai.devops.modules.infrastructure.container.service;

import ai.devops.common.exception.ApprovalRequiredException;
import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.repository.ContainerRepository;
import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ContainerService {

    private static final Logger log = LoggerFactory.getLogger(ContainerService.class);

    private final ContainerRepository containerRepository;
    private final ApprovalWorkflowService approvalService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public ContainerService(
            ContainerRepository containerRepository,
            ApprovalWorkflowService approvalService,
            AuditService auditService,
            UserRepository userRepository) {
        this.containerRepository = containerRepository;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ContainerEntity> getContainers(UUID envId) {
        if (envId != null) {
            return containerRepository.findByEnvironmentId(envId);
        }
        return containerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ContainerEntity getContainerById(UUID id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Container", id));
    }

    @Transactional(readOnly = true)
    public List<String> getContainerLogs(UUID id, int tail) {
        ContainerEntity container = getContainerById(id);
        List<String> logs = new ArrayList<>();
        Instant now = Instant.now();

        if ("thingsboard-core-app".equalsIgnoreCase(container.getName())) {
            logs.add(String.format("[%s] [INFO] [o.t.s.t.ThingsboardServer] Starting Thingsboard Server Application v3.6.2...", now.minusSeconds(300)));
            logs.add(String.format("[%s] [INFO] [o.t.s.d.HikariDataSource] HikariPool-1 - Starting...", now.minusSeconds(290)));
            logs.add(String.format("[%s] [INFO] [o.t.s.d.HikariDataSource] HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@1a2b3c", now.minusSeconds(285)));
            logs.add(String.format("[%s] [WARN] [o.t.s.t.p.DefaultTbQueueProducer] Telemetry queue buffer saturation at 98%%", now.minusSeconds(120)));
            logs.add(String.format("[%s] [ERROR] [o.t.s.d.HikariDataSource] HikariPool-1 - Connection is not available, request timed out after 30005ms.", now.minusSeconds(45)));
            logs.add(String.format("[%s] [ERROR] [o.t.s.e.GlobalExceptionHandler] org.springframework.dao.CannotAcquireLockException: could not execute statement; SQL [UPDATE ts_kv SET ...]", now.minusSeconds(30)));
            logs.add(String.format("[%s] [ERROR] [o.t.s.t.TransportService] Fatal thread starvation in event processor. Initiating emergency shutdown.", now.minusSeconds(10)));
            logs.add(String.format("[%s] [WARN] [o.t.s.t.ThingsboardServer] Exiting with code 137 (OOM / Thread Deadlock)", now.minusSeconds(2)));
        } else {
            logs.add(String.format("[%s] [INFO] Service %s started successfully", now.minusSeconds(3600), container.getName()));
            logs.add(String.format("[%s] [INFO] Health check probe OK (200 OK, latency=4ms)", now.minusSeconds(60)));
            logs.add(String.format("[%s] [INFO] Active connections: 14, Queue depth: 0", now.minusSeconds(10)));
        }

        return logs;
    }

    @Transactional
    public Map<String, Object> executeContainerAction(UUID id, String action, boolean isApproved) {
        if (!SecurityUtils.isDevopsEngineer()) {
            throw new UnauthorizedActionException("Container mutation actions require DEVOPS_ENGINEER or ADMIN role");
        }

        ContainerEntity container = getContainerById(id);
        boolean isProd = container.getEnvironment().isProduction();
        RiskLevel risk = isProd ? RiskLevel.HIGH_RISK : RiskLevel.LOW_RISK;

        if (isProd && !isApproved) {
            String currentUserEmail = SecurityUtils.getCurrentUserEmail();
            UserEntity user = userRepository.findByEmail(currentUserEmail).orElse(null);

            ApprovalRequestEntity approval = approvalService.createApprovalRequest(
                    null,
                    container.getEnvironment(),
                    user,
                    action + "_container",
                    String.format("User requested %s on production container '%s' (%s)", action, container.getName(), container.getContainerId()),
                    "Possible transient service interruption while container restarts/stops"
            );

            throw new ApprovalRequiredException(action + "_container", container.getEnvironment().getName(), risk, approval.getId());
        }

        // Execute action
        switch (action.toLowerCase()) {
            case "restart" -> {
                container.setState("RUNNING");
                container.setStartedAt(Instant.now());
                if (container.getRestartCount() > 0) {
                    container.setRestartCount(container.getRestartCount() + 1);
                }
            }
            case "stop" -> container.setState("EXITED");
            case "start" -> {
                container.setState("RUNNING");
                container.setStartedAt(Instant.now());
            }
            default -> throw new IllegalArgumentException("Unsupported container action: " + action);
        }

        containerRepository.save(container);

        auditService.recordAudit(
                action.toUpperCase() + "_CONTAINER",
                "CONTAINER",
                container.getName(),
                container.getEnvironment().getName(),
                risk,
                String.format("action=%s, id=%s", action, container.getId()),
                "SUCCESS",
                null,
                null
        );

        Map<String, Object> result = new HashMap<>();
        result.put("containerId", container.getId());
        result.put("name", container.getName());
        result.put("action", action);
        result.put("status", "SUCCESS");
        result.put("newState", container.getState());
        result.put("timestamp", Instant.now());
        return result;
    }
}
