package ai.devops.modules.integration.core;

import java.util.Map;

public interface InfrastructureIntegration {
    IntegrationType getType();
    boolean testConnection(String endpointUrl, String configEncrypted);
    Map<String, Object> collectHealth(String endpointUrl, String configEncrypted);
}
