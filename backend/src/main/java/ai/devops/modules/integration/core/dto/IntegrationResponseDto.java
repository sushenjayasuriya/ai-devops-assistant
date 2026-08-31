package ai.devops.modules.integration.core.dto;

import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationResponseDto {
    private UUID id;
    private UUID environmentId;
    private String environmentName;
    private boolean isProduction;
    private IntegrationType type;
    private String name;
    private String endpointUrl;
    private String authType;
    private Integer timeoutMs;
    private boolean enabled;
    private String healthStatus;
    private Instant lastSyncedAt;
    private Instant createdAt;

    public IntegrationResponseDto() {}

    public static IntegrationResponseDto fromEntity(IntegrationEntity entity) {
        IntegrationResponseDto dto = new IntegrationResponseDto();
        dto.setId(entity.getId());
        if (entity.getEnvironment() != null) {
            dto.setEnvironmentId(entity.getEnvironment().getId());
            dto.setEnvironmentName(entity.getEnvironment().getName());
            dto.setProduction(entity.getEnvironment().isProduction());
        }
        dto.setType(entity.getType());
        dto.setName(entity.getName());
        dto.setEndpointUrl(entity.getEndpointUrl());
        dto.setAuthType(entity.getAuthType());
        dto.setTimeoutMs(entity.getTimeoutMs());
        dto.setEnabled(entity.isEnabled());
        dto.setHealthStatus(entity.getHealthStatus());
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public boolean isProduction() {
        return isProduction;
    }

    public void setProduction(boolean production) {
        isProduction = production;
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

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
