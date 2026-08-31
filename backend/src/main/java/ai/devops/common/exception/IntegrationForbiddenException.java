package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class IntegrationForbiddenException extends BaseException {
    public IntegrationForbiddenException(String integrationType, String message) {
        super("INTEGRATION_FORBIDDEN",
                String.format("Access to integration [%s] was forbidden: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }
}
