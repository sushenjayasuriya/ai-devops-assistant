package ai.devops.common.exception;

import java.util.Map;

public class BaseException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BaseException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
