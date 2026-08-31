package ai.devops.security.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SecretCryptoService {

    private static final Logger log = LoggerFactory.getLogger(SecretCryptoService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bit auth tag

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    @org.springframework.beans.factory.annotation.Autowired
    public SecretCryptoService(
            @Value("${app.encryption.key:${APP_ENCRYPTION_KEY:}}") String encryptionKeyConfig,
            @Value("${spring.profiles.active:dev}") String activeProfile) {

        this.secureRandom = new SecureRandom();

        byte[] keyBytes = parseKeyBytes(encryptionKeyConfig);

        if (keyBytes == null || keyBytes.length != 32) {
            if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
                throw new IllegalStateException("FATAL: In production mode, APP_ENCRYPTION_KEY must be a valid 256-bit key (32 bytes base64 or 64-character hex).");
            }
            log.warn("WARNING: Using development fallback encryption master key. DO NOT USE IN PRODUCTION.");
            keyBytes = "dev-master-encryption-key-32-byte!".getBytes(StandardCharsets.UTF_8);
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // Constructor for testing with explicit key
    public SecretCryptoService(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 bytes (256 bits).");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt secret payload", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            if (decoded.length < GCM_IV_LENGTH + 16) { // Minimum 12-byte IV + 16-byte tag
                throw new IllegalArgumentException("Ciphertext payload is truncated or malformed.");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret payload: Ciphertext may be corrupted, tampered, or key mismatch", e);
        }
    }

    private static byte[] parseKeyBytes(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        rawKey = rawKey.trim();
        // Try Base64
        try {
            byte[] decoded = Base64.getDecoder().decode(rawKey);
            if (decoded.length == 32) return decoded;
        } catch (Exception ignored) {}

        // Try Hex
        try {
            if (rawKey.length() == 64) {
                return HexFormat.of().parseHex(rawKey);
            }
        } catch (Exception ignored) {}

        // Try UTF-8 direct byte length
        byte[] utf8Bytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length == 32) {
            return utf8Bytes;
        }

        return null;
    }
}
