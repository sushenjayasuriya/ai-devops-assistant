package ai.devops.modules.ai.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GuardrailVerificationTest {

    private GuardrailVerificationEngine guardrailEngine;

    @BeforeEach
    void setUp() {
        guardrailEngine = new GuardrailVerificationEngine();
    }

    @Test
    @DisplayName("Should cap confidence and flag unbacked findings when AI executes no telemetry tools")
    void testGuardrailRejectsEmptyToolExecution() {
        FOIRResponse response = new FOIRResponse();
        response.setConfidenceScore(0.95);
        response.setFacts(List.of("Fabricated fact: CPU is 99%"));

        FOIRResponse result = guardrailEngine.verifyAndSanitize(response, List.of());

        assertTrue(result.getConfidenceScore() <= 0.35);
        assertTrue(result.getFacts().get(0).contains("Notice: No live telemetry tools were executed"));
    }

    @Test
    @DisplayName("Should pass verification when tool calls are successful")
    void testGuardrailPassesValidToolCalls() {
        FOIRResponse response = new FOIRResponse();
        response.setConfidenceScore(0.91);
        response.setFacts(List.of("Prometheus metric verified: CPU is 94.2%"));

        List<FOIRResponse.ToolCallRecord> tools = List.of(
                new FOIRResponse.ToolCallRecord("query_prometheus", Map.of(), Map.of("value", 94.2), true)
        );

        FOIRResponse result = guardrailEngine.verifyAndSanitize(response, tools);

        assertEquals(0.91, result.getConfidenceScore(), 0.001);
        assertEquals(1, result.getFacts().size());
    }
}
