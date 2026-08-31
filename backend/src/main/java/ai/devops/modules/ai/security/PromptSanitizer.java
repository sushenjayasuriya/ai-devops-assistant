package ai.devops.modules.ai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class PromptSanitizer {

    private static final Logger log = LoggerFactory.getLogger(PromptSanitizer.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(all\\s+)?(previous|prior)\\s+system\\s+prompts?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+in\\s+(dan|developer|jailbreak)\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s*:\\s*override", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\|im_start\\|>system", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[INST\\]\\s*<<SYS>>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("print\\s+(your|the)\\s+(initial|system)\\s+prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal\\s+(the\\s+)?(master|app_encryption|jwt)\\s+key", Pattern.CASE_INSENSITIVE)
    );

    public String sanitizePrompt(String rawPrompt) {
        if (rawPrompt == null || rawPrompt.isBlank()) {
            return "";
        }

        String cleaned = rawPrompt.trim();

        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(cleaned).find()) {
                log.warn("SECURITY ALERT: Indirect Prompt Injection pattern detected in prompt: [{}]", p.pattern());
                cleaned = p.matcher(cleaned).replaceAll("[BLOCKED_INJECTION_ATTEMPT]");
            }
        }

        // Limit maximum prompt length to prevent token flood attacks
        if (cleaned.length() > 4000) {
            log.warn("Truncating oversized prompt length from {} to 4000 characters", cleaned.length());
            cleaned = cleaned.substring(0, 4000);
        }

        return cleaned;
    }
}
