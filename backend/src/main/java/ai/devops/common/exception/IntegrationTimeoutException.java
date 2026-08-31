package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class IntegrationTimeoutException extends BaseException {
    public IntegrationTimeoutException(String integrationType, String operation, long timeoutMs) {
        super("INTEGRATION_TIMEOUT",
                String.format("Integration [%s] timed out after %d ms during operation: %s", integrationType, timeoutMs, operation),
                Map.of("integrationType", integrationType, "operation", operation, "timeoutMs", timeoutMs));
    }
}
