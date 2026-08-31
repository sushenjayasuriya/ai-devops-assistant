package ai.devops.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SsrfProtectionException extends BaseException {
    public SsrfProtectionException(String url, String reason) {
        super("SSRF_ATTEMPT_BLOCKED",
                String.format("SSRF Protection blocked request to endpoint '%s': %s", url, reason),
                Map.of("endpoint", url != null ? url : "unknown", "reason", reason != null ? reason : "unauthorized endpoint"));
    }
}
