package ai.devops.modules.deployment.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "deployments")
public class DeploymentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "version_tag", nullable = false)
    private String versionTag;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "deployed_by")
    private String deployedBy;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, IN_PROGRESS, ROLLED_BACK

    @Column(name = "changelog", columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public DeploymentEntity() {}

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersionTag() {
        return versionTag;
    }

    public void setVersionTag(String versionTag) {
        this.versionTag = versionTag;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getDeployedBy() {
        return deployedBy;
    }

    public void setDeployedBy(String deployedBy) {
        this.deployedBy = deployedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
