package ai.devops.common.exception;

import java.util.Map;

public class UnauthorizedActionException extends BaseException {
    public UnauthorizedActionException(String message) {
        super("UNAUTHORIZED_ACTION", message);
    }

    public UnauthorizedActionException(String action, String requiredRole) {
        super("UNAUTHORIZED_ACTION", String.format("Action '%s' requires '%s' role permission", action, requiredRole),
                Map.of("action", action, "requiredRole", requiredRole));
    }
}
