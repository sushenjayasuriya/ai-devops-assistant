package ai.devops.common.exception;

import java.util.Map;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s with identifier '%s' was not found", resourceName, identifier),
                Map.of("resource", resourceName, "identifier", String.valueOf(identifier)));
    }
}
