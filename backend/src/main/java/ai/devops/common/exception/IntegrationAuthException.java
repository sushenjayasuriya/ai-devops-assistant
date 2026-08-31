package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class IntegrationAuthException extends BaseException {
    public IntegrationAuthException(String integrationType, String message) {
        super("INTEGRATION_AUTH_FAILED",
                String.format("Authentication to integration [%s] failed: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }
}
