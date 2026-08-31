package ai.devops.modules.integration.prometheus;

import ai.devops.common.exception.IntegrationInvalidResponseException;
import ai.devops.common.exception.SsrfProtectionException;
import ai.devops.modules.integration.prometheus.client.PrometheusHttpClient;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusHttpClientTest {

    private SsrfProtectionValidator ssrfValidator;
    private PrometheusHttpClient client;

    @BeforeEach
    void setUp() {
        ssrfValidator = new SsrfProtectionValidator(true);
        client = new PrometheusHttpClient(ssrfValidator);
    }

    @Test
    @DisplayName("SSRF protection blocks cloud metadata IP addresses")
    void testSsrfBlocksCloudMetadata() {
        assertThrows(SsrfProtectionException.class, () ->
                client.executeInstantQuery("http://169.254.169.254/latest/meta-data", null, "up", Instant.now(), 2000)
        );
    }

    @Test
    @DisplayName("Connection test handles offline/unreachable Prometheus gracefully")
    void testConnectionOffline() {
        var result = client.testConnection("http://localhost:59999", null);
        assertNotNull(result);
        assertEquals("UNHEALTHY", result.get("status"));
        assertEquals(false, result.get("connected"));
    }
}
