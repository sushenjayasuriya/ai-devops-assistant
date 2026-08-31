package ai.devops.modules.ai.llm;

import ai.devops.common.model.RiskLevel;
import ai.devops.modules.ai.llm.provider.GeminiLlmClient;
import ai.devops.modules.ai.tools.DevOpsTool;
import ai.devops.modules.ai.tools.ToolExecutionResult;
import ai.devops.security.rbac.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiLlmClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiLlmClient geminiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        geminiClient = new GeminiLlmClient(
                "fake-gemini-key",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta",
                restTemplate,
                objectMapper
        );
    }

    @Test
    @DisplayName("Parse Gemini function call response correctly")
    void testParseFunctionCallResponse() {
        String mockGeminiResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "I will query Prometheus to inspect container CPU load."
                  },
                  {
                    "functionCall": {
                      "name": "query_prometheus",
                      "args": {
                        "query": "container_cpu_usage_percent{container=\\"app\\"}"
                      }
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockGeminiResponse, HttpStatus.OK));

        DevOpsTool testTool = new DevOpsTool() {
            @Override
            public String getName() { return "query_prometheus"; }
            @Override
            public String getDescription() { return "Query Prometheus"; }
            @Override
            public Map<String, String> getParameterSchema() { return Map.of("query", "PromQL query string"); }
            @Override
            public Set<String> getRequiredParameters() { return Set.of("query"); }
            @Override
            public Set<String> getAllowedParameters() { return Set.of("query"); }
            @Override
            public RiskLevel getRiskLevel() { return RiskLevel.LOW_RISK; }
            @Override
            public Role getRequiredRole() { return Role.VIEWER; }
            @Override
            public boolean isReadOnly() { return true; }
            @Override
            public boolean requiresProductionApproval() { return false; }
            @Override
            public ToolExecutionResult execute(Map<String, Object> parameters) {
                return ToolExecutionResult.ok("query_prometheus", "[]");
            }
        };

        LlmResponse response = geminiClient.generateChat(List.of(LlmMessage.user("Check CPU")), List.of(testTool));

        assertTrue(response.hasToolCalls());
        assertEquals(1, response.getToolCalls().size());
        assertEquals("query_prometheus", response.getToolCalls().get(0).getName());
        assertEquals("container_cpu_usage_percent{container=\"app\"}", response.getToolCalls().get(0).getArguments().get("query"));
    }

    @Test
    @DisplayName("Parse Gemini text synthesis response correctly")
    void testParseTextSynthesisResponse() {
        String mockGeminiResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "FACTS:\\n- CPU at 94.2%\\n\\nOBSERVATIONS:\\n- Deadlock detected\\n\\nINFERENCES:\\n- Connection pool exhausted\\n\\nRECOMMENDATIONS:\\n- Restart container"
                  }
                ]
              }
            }
          ]
        }
        """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(mockGeminiResponse, HttpStatus.OK));

        LlmResponse response = geminiClient.generateChat(List.of(LlmMessage.user("Analyze incident")), List.of());

        assertFalse(response.hasToolCalls());
        assertTrue(response.getContent().contains("FACTS:"));
        assertTrue(response.getContent().contains("RECOMMENDATIONS:"));
    }
}
