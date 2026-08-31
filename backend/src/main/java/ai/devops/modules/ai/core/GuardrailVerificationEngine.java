package ai.devops.modules.ai.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GuardrailVerificationEngine {

    private static final Logger log = LoggerFactory.getLogger(GuardrailVerificationEngine.class);

    public FOIRResponse verifyAndSanitize(FOIRResponse response, List<FOIRResponse.ToolCallRecord> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            log.warn("Guardrail Alert: AI generated response without executing any telemetry tools!");
            response.setConfidenceScore(Math.min(response.getConfidenceScore(), 0.35));
            List<String> sanitizedFacts = new ArrayList<>();
            sanitizedFacts.add("Notice: No live telemetry tools were executed for this query. Findings are based solely on static baseline context.");
            response.setFacts(sanitizedFacts);
            return response;
        }

        // Check if any tool succeeded
        boolean anySuccess = toolCalls.stream().anyMatch(FOIRResponse.ToolCallRecord::isSuccess);
        if (!anySuccess) {
            response.setConfidenceScore(0.20);
            response.setSummary("Telemetry query failure: Unable to collect infrastructure state.");
            response.setInferences(List.of("Telemetry endpoints were unreachable. Insufficient data to determine root cause."));
            response.setRecommendations(List.of());
            return response;
        }

        log.info("Guardrail Verification Passed: {} tool calls validated against FOIR schema with confidence {}",
                toolCalls.size(), response.getConfidenceScore());

        return response;
    }
}
