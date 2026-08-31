package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class IntegrationUnavailableException extends BaseException {
    public IntegrationUnavailableException(String integrationType, String message) {
        super("INTEGRATION_UNAVAILABLE", String.format("Integration [%s] is unavailable: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }

    public IntegrationUnavailableException(String integrationType, String message, Throwable cause) {
        super("INTEGRATION_UNAVAILABLE", String.format("Integration [%s] is unavailable: %s", integrationType, message),
                Map.of("integrationType", integrationType, "cause", cause.getMessage() != null ? cause.getMessage() : "unknown"));
    }
}
