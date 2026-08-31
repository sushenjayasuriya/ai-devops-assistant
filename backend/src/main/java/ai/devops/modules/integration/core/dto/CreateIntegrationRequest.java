package ai.devops.modules.integration.core.dto;

import ai.devops.modules.integration.core.IntegrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateIntegrationRequest {

    @NotNull(message = "Environment ID is required")
    private UUID environmentId;

    @NotNull(message = "Integration type is required")
    private IntegrationType type;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Endpoint URL is required")
    private String endpointUrl;

    private String authType = "NONE";
    private String configRaw;
    private Integer timeoutMs = 5000;
    private boolean enabled = true;

    public CreateIntegrationRequest() {}

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
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

    public String getConfigRaw() {
        return configRaw;
    }

    public void setConfigRaw(String configRaw) {
        this.configRaw = configRaw;
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
}
