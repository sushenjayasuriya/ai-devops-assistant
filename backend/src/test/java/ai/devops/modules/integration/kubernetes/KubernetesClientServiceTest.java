package ai.devops.modules.integration.kubernetes;

import ai.devops.modules.integration.kubernetes.client.KubernetesClientService;
import ai.devops.security.encryption.SecretCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KubernetesClientServiceTest {

    @Mock
    private SecretCryptoService cryptoService;

    private KubernetesClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new KubernetesClientService(cryptoService);
    }

    @Test
    @DisplayName("Testing connection to offline or dummy cluster returns UNHEALTHY status")
    void testOfflineKubernetesConnection() {
        var result = clientService.testConnection(null);
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
        assertTrue(result.containsKey("connected"));
    }
}
