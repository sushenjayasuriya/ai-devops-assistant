package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class IntegrationInvalidResponseException extends BaseException {
    public IntegrationInvalidResponseException(String integrationType, String message) {
        super("INTEGRATION_INVALID_RESPONSE",
                String.format("Invalid or malformed response from integration [%s]: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }
}
