package ai.devops.modules.ai.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SecretMasker {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            // Private Keys
            Pattern.compile("-----BEGIN[ A-Z0-9_-]+KEY-----[\\s\\S]*?-----END[ A-Z0-9_-]+KEY-----"),
            // JWT Tokens
            Pattern.compile("eyJ[a-zA-Z0-9_-]{10,}\\.eyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}"),
            // Bearer Tokens
            Pattern.compile("Bearer\\s+[a-zA-Z0-9_.-]{20,}", Pattern.CASE_INSENSITIVE),
            // Passwords & Secrets in JSON
            Pattern.compile("(\"(?:password|secret|apiKey|api_key|token|privateKey)\"\\s*:\\s*\")[^\"]+(\")", Pattern.CASE_INSENSITIVE),
            // AWS Keys
            Pattern.compile("(?:AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}")
    );

    public String maskSecrets(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;
        for (Pattern p : SECRET_PATTERNS) {
            masked = p.matcher(masked).replaceAll("[REDACTED_SECRET]");
        }
        return masked;
    }
}
