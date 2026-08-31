package ai.devops.modules.integration.core.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.integration.core.IntegrationType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "integrations")
public class IntegrationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IntegrationType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "endpoint_url", nullable = false)
    private String endpointUrl;

    @Column(name = "auth_type", nullable = false)
    private String authType = "NONE"; // NONE, BEARER_TOKEN, BASIC_AUTH, SSH_KEY, KUBECONFIG

    @Column(name = "config_encrypted", columnDefinition = "TEXT")
    private String configEncrypted;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 5000;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 2;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "health_status", nullable = false)
    private String healthStatus = "UNCONFIGURED"; // HEALTHY, UNHEALTHY, UNCONFIGURED

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public IntegrationEntity() {}

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getConfigEncrypted() {
        return configEncrypted;
    }

    public void setConfigEncrypted(String configEncrypted) {
        this.configEncrypted = configEncrypted;
    }

    public Integer getTimeoutMs() {
        return timeoutMs != null ? timeoutMs : 5000;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMaxRetries() {
        return maxRetries != null ? maxRetries : 2;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
