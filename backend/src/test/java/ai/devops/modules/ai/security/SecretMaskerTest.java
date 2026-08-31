package ai.devops.modules.ai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretMaskerTest {

    private SecretMasker secretMasker;

    @BeforeEach
    void setUp() {
        secretMasker = new SecretMasker();
    }

    @Test
    @DisplayName("Mask private RSA keys from LLM payloads")
    void testMaskPrivateKey() {
        String payload = "Host config: -----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY----- status: ok";
        String masked = secretMasker.maskSecrets(payload);

        assertFalse(masked.contains("BEGIN RSA PRIVATE KEY"));
        assertTrue(masked.contains("[REDACTED_SECRET]"));
    }

    @Test
    @DisplayName("Mask JWT tokens")
    void testMaskJwtToken() {
        String payload = "Auth header: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMDAwMDAwMCJ9.abcdef1234567890";
        String masked = secretMasker.maskSecrets(payload);

        assertFalse(masked.contains("eyJhbGciOiJIUzUxMiJ9"));
        assertTrue(masked.contains("[REDACTED_SECRET]"));
    }

    @Test
    @DisplayName("Mask passwords in JSON")
    void testMaskPasswordJson() {
        String payload = "{\"user\":\"admin\", \"password\": \"SuperSecretPassword123!\"}";
        String masked = secretMasker.maskSecrets(payload);

        assertFalse(masked.contains("SuperSecretPassword123!"));
        assertTrue(masked.contains("[REDACTED_SECRET]"));
    }
}
