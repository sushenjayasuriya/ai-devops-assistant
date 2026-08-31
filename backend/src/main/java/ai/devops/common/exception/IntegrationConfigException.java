package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IntegrationConfigException extends BaseException {
    public IntegrationConfigException(String integrationType, String message) {
        super("INTEGRATION_CONFIGURATION_ERROR",
                String.format("Invalid configuration for integration [%s]: %s", integrationType, message),
                Map.of("integrationType", integrationType));
    }
}
