package ai.devops.security.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SecretCryptoTest {

    private final byte[] keyA = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
    private final byte[] keyB = "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("Should successfully encrypt and decrypt string with AES-256-GCM")
    void testEncryptDecryptSuccess() {
        SecretCryptoService cryptoService = new SecretCryptoService(keyA);
        String secret = "postgres://admin:SuperSecretPass123!@db.internal:5432/proddb";

        String encrypted = cryptoService.encrypt(secret);
        assertNotNull(encrypted);
        assertNotEquals(secret, encrypted);

        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(secret, decrypted);
    }

    @Test
    @DisplayName("Should generate different ciphertexts for the same plaintext due to random IV")
    void testUniqueIvPerEncryption() {
        SecretCryptoService cryptoService = new SecretCryptoService(keyA);
        String secret = "SameSecretContent";

        String enc1 = cryptoService.encrypt(secret);
        String enc2 = cryptoService.encrypt(secret);

        assertNotEquals(enc1, enc2);
        assertEquals(secret, cryptoService.decrypt(enc1));
        assertEquals(secret, cryptoService.decrypt(enc2));
    }

    @Test
    @DisplayName("Should fail decryption when ciphertext is tampered")
    void testTamperedCiphertextFails() {
        SecretCryptoService cryptoService = new SecretCryptoService(keyA);
        String encrypted = cryptoService.encrypt("SensitiveData");

        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length - 1] ^= 0x55; // Flip bit in auth tag
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(tampered));
    }

    @Test
    @DisplayName("Should fail decryption when using wrong encryption key")
    void testWrongKeyFails() {
        SecretCryptoService serviceA = new SecretCryptoService(keyA);
        SecretCryptoService serviceB = new SecretCryptoService(keyB);

        String encrypted = serviceA.encrypt("EncryptedWithKeyA");
        assertThrows(RuntimeException.class, () -> serviceB.decrypt(encrypted));
    }

    @Test
    @DisplayName("Should fail initialization if key length is not exactly 32 bytes")
    void testInvalidKeyLengthRejected() {
        byte[] shortKey = "too-short".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new SecretCryptoService(shortKey));
    }
}
