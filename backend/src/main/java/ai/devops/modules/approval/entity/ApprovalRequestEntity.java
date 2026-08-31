package ai.devops.modules.approval.entity;

import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.user.entity.OrganizationEntity;
import ai.devops.modules.user.entity.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "ai_action_id")
    private UUID aiActionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserEntity requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private UserEntity resolvedByUser;

    @Column(name = "action_type", nullable = false)
    private String actionType; // restart_container, stop_container, start_container, rollback_deployment

    @Column(name = "target_resource_type")
    private String targetResourceType; // CONTAINER, SERVER, DEPLOYMENT

    @Column(name = "target_resource_id")
    private String targetResourceId;

    @Column(name = "target_resource_name")
    private String targetResourceName;

    @Column(name = "action_parameters", columnDefinition = "TEXT")
    private String actionParameters; // Immutable snapshot of arguments

    @Column(name = "rationale", columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Column(name = "expected_impact", columnDefinition = "TEXT", nullable = false)
    private String expectedImpact;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPIRED, EXECUTED, CANCELLED

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = requestedAt.plusSeconds(3600); // 1 hour default TTL
        }
    }

    public ApprovalRequestEntity() {}

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public UUID getAiActionId() {
        return aiActionId;
    }

    public void setAiActionId(UUID aiActionId) {
        this.aiActionId = aiActionId;
    }

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public UserEntity getRequestedByUser() {
        return requestedByUser;
    }

    public void setRequestedByUser(UserEntity requestedByUser) {
        this.requestedByUser = requestedByUser;
    }

    public UserEntity getResolvedByUser() {
        return resolvedByUser;
    }

    public void setResolvedByUser(UserEntity resolvedByUser) {
        this.resolvedByUser = resolvedByUser;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(String targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public String getTargetResourceId() {
        return targetResourceId;
    }

    public void setTargetResourceId(String targetResourceId) {
        this.targetResourceId = targetResourceId;
    }

    public String getTargetResourceName() {
        return targetResourceName;
    }

    public void setTargetResourceName(String targetResourceName) {
        this.targetResourceName = targetResourceName;
    }

    public String getActionParameters() {
        return actionParameters;
    }

    public void setActionParameters(String actionParameters) {
        this.actionParameters = actionParameters;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getExpectedImpact() {
        return expectedImpact;
    }

    public void setExpectedImpact(String expectedImpact) {
        this.expectedImpact = expectedImpact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
