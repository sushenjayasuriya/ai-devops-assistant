package ai.devops.modules.integration.core.dto;

import ai.devops.modules.integration.core.IntegrationType;

import java.time.Instant;
import java.util.UUID;

public class TestConnectionResult {
    private UUID integrationId;
    private String name;
    private IntegrationType type;
    private boolean connected;
    private String status; // HEALTHY, UNHEALTHY, DEGRADED
    private Long latencyMs;
    private Instant checkedAt;
    private String errorCode;
    private String errorMessage;

    public TestConnectionResult() {
        this.checkedAt = Instant.now();
    }

    public TestConnectionResult(UUID integrationId, String name, IntegrationType type, boolean connected, String status, Long latencyMs, String errorCode, String errorMessage) {
        this.integrationId = integrationId;
        this.name = name;
        this.type = type;
        this.connected = connected;
        this.status = status;
        this.latencyMs = latencyMs;
        this.checkedAt = Instant.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public UUID getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(UUID integrationId) {
        this.integrationId = integrationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
