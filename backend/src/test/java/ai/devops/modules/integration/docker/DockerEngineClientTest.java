package ai.devops.modules.integration.docker;

import ai.devops.common.exception.IntegrationUnavailableException;
import ai.devops.common.exception.SsrfProtectionException;
import ai.devops.modules.integration.docker.client.DockerEngineClient;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerEngineClientTest {

    private SsrfProtectionValidator ssrfValidator;
    private DockerEngineClient client;

    @BeforeEach
    void setUp() {
        ssrfValidator = new SsrfProtectionValidator(true);
        client = new DockerEngineClient(ssrfValidator, new ObjectMapper(), "tcp://localhost:2375");
    }

    @Test
    @DisplayName("SSRF protection blocks cloud metadata in Docker endpoint")
    void testSsrfBlocksCloudMetadata() {
        assertThrows(SsrfProtectionException.class, () ->
                client.listContainers("http://169.254.169.254", false)
        );
    }

    @Test
    @DisplayName("Offline Docker daemon returns UNHEALTHY status and connected=false")
    void testOfflineDockerDaemon() {
        var result = client.testConnection("tcp://localhost:59998");
        assertNotNull(result);
        assertEquals("UNHEALTHY", result.get("status"));
        assertEquals(false, result.get("connected"));
    }

    @Test
    @DisplayName("Listing containers on offline Docker daemon throws IntegrationUnavailableException")
    void testListContainersOffline() {
        assertThrows(IntegrationUnavailableException.class, () ->
                client.listContainers("tcp://localhost:59998", false)
        );
    }
}
