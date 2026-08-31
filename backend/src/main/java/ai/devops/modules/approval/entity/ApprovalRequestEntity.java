package ai.devops.modules.approval.entity;

import ai.devops.modules.environment.entity.EnvironmentEntity;
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
    private String actionType; // e.g. restart_container, rollback_deployment

    @Column(name = "rationale", columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Column(name = "expected_impact", columnDefinition = "TEXT", nullable = false)
    private String expectedImpact;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPIRED

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }

    public ApprovalRequestEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
