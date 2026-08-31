package ai.devops.common.exception;

import java.util.Map;

public class IntegrationException extends BaseException {
    public IntegrationException(String integrationType, String message) {
        super("INTEGRATION_ERROR", String.format("Integration [%s] error: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }

    public IntegrationException(String integrationType, String message, Throwable cause) {
        super("INTEGRATION_ERROR", String.format("Integration [%s] error: %s", integrationType, message),
                Map.of("integrationType", integrationType, "cause", cause.getMessage() != null ? cause.getMessage() : "unknown"));
    }
}
