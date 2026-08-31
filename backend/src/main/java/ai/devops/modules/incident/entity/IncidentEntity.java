package ai.devops.modules.incident.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.incident.IncidentSeverity;
import ai.devops.modules.incident.IncidentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class IncidentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private IncidentSeverity severity = IncidentSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "affected_resource_type")
    private String affectedResourceType;

    @Column(name = "affected_resource_id")
    private String affectedResourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_cause_deployment_id")
    private DeploymentEntity rootCauseDeployment;

    @Column(name = "root_cause_summary", columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentEventEntity> events = new ArrayList<>();

    public IncidentEntity() {}

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getAffectedResourceType() {
        return affectedResourceType;
    }

    public void setAffectedResourceType(String affectedResourceType) {
        this.affectedResourceType = affectedResourceType;
    }

    public String getAffectedResourceId() {
        return affectedResourceId;
    }

    public void setAffectedResourceId(String affectedResourceId) {
        this.affectedResourceId = affectedResourceId;
    }

    public DeploymentEntity getRootCauseDeployment() {
        return rootCauseDeployment;
    }

    public void setRootCauseDeployment(DeploymentEntity rootCauseDeployment) {
        this.rootCauseDeployment = rootCauseDeployment;
    }

    public String getRootCauseSummary() {
        return rootCauseSummary;
    }

    public void setRootCauseSummary(String rootCauseSummary) {
        this.rootCauseSummary = rootCauseSummary;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<IncidentEventEntity> getEvents() {
        return events;
    }

    public void setEvents(List<IncidentEventEntity> events) {
        this.events = events;
    }
}
