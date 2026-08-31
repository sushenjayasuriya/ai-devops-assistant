package ai.devops.modules.integration.linux;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.linux.client.LinuxSshClient;
import ai.devops.modules.integration.linux.model.LinuxCommand;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import ai.devops.security.encryption.SecretCryptoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class LinuxServerIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(LinuxServerIntegration.class);

    private final LinuxSshClient sshClient;
    private final SecretCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public LinuxServerIntegration(LinuxSshClient sshClient, SecretCryptoService cryptoService, ObjectMapper objectMapper) {
        this.sshClient = sshClient;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.LINUX_SSH;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        HostConfig config = parseEndpointAndConfig(endpointUrl, configEncrypted);
        Map<String, Object> result = sshClient.testConnection(config.host, config.port, config.user, config.password, config.privateKey);
        return Boolean.TRUE.equals(result.get("connected"));
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        HostConfig config = parseEndpointAndConfig(endpointUrl, configEncrypted);
        return sshClient.testConnection(config.host, config.port, config.user, config.password, config.privateKey);
    }

    public LinuxServerTelemetry collectTelemetry(String endpointUrl, String configEncrypted, int timeoutMs) {
        HostConfig config = parseEndpointAndConfig(endpointUrl, configEncrypted);
        return sshClient.collectServerTelemetry(config.host, config.port, config.user, config.password, config.privateKey, timeoutMs > 0 ? timeoutMs : 5000);
    }

    public String executeTypedCommand(String endpointUrl, String configEncrypted, LinuxCommand command, int timeoutMs) {
        HostConfig config = parseEndpointAndConfig(endpointUrl, configEncrypted);
        return sshClient.executeTypedCommand(config.host, config.port, config.user, config.password, config.privateKey, command, timeoutMs > 0 ? timeoutMs : 5000);
    }

    private HostConfig parseEndpointAndConfig(String endpointUrl, String configEncrypted) {
        String host = "localhost";
        int port = 22;
        String user = "devops";
        String password = null;
        String privateKey = null;

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            String trimmed = endpointUrl.replace("ssh://", "");
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":");
                host = parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            } else {
                host = trimmed;
            }
        }

        if (configEncrypted != null && !configEncrypted.isBlank()) {
            try {
                String decrypted = cryptoService.decrypt(configEncrypted);
                if (decrypted != null && decrypted.startsWith("{")) {
                    Map<String, Object> map = objectMapper.readValue(decrypted, Map.class);
                    if (map.containsKey("user")) user = String.valueOf(map.get("user"));
                    if (map.containsKey("password")) password = String.valueOf(map.get("password"));
                    if (map.containsKey("privateKey")) privateKey = String.valueOf(map.get("privateKey"));
                    if (map.containsKey("port")) port = Integer.parseInt(String.valueOf(map.get("port")));
                } else if (decrypted != null) {
                    password = decrypted;
                }
            } catch (Exception ex) {
                log.warn("Failed to decrypt SSH credentials: {}", ex.getMessage());
            }
        }

        return new HostConfig(host, port, user, password, privateKey);
    }

    private record HostConfig(String host, int port, String user, String password, String privateKey) {}
}
