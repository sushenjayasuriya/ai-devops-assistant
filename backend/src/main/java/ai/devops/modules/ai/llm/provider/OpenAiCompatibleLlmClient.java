package ai.devops.modules.ai.llm.provider;

import ai.devops.modules.ai.llm.LlmClient;
import ai.devops.modules.ai.llm.LlmMessage;
import ai.devops.modules.ai.llm.LlmResponse;
import ai.devops.modules.ai.llm.LlmToolCall;
import ai.devops.modules.ai.tools.DevOpsTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiCompatibleLlmClient(
            @Value("${app.ai.openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${app.ai.openai.model:gpt-4o}") String model,
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("mock-key");
    }

    @Override
    public LlmResponse generateChat(List<LlmMessage> messages, List<DevOpsTool> availableTools) {
        try {
            String endpoint = baseUrl + "/chat/completions";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (LlmMessage msg : messages) {
                ObjectNode msgObj = messagesArray.addObject();
                msgObj.put("role", msg.getRole().name().toLowerCase());
                msgObj.put("content", msg.getContent() != null ? msg.getContent() : "");

                if (msg.getRole() == LlmMessage.Role.TOOL) {
                    msgObj.put("tool_call_id", msg.getToolCallId() != null ? msg.getToolCallId() : "call_1");
                    msgObj.put("name", msg.getToolName());
                } else if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    ArrayNode tcArray = msgObj.putArray("tool_calls");
                    for (LlmToolCall tc : msg.getToolCalls()) {
                        ObjectNode tcNode = tcArray.addObject();
                        tcNode.put("id", tc.getId());
                        tcNode.put("type", "function");
                        ObjectNode fnNode = tcNode.putObject("function");
                        fnNode.put("name", tc.getName());
                        fnNode.put("arguments", objectMapper.writeValueAsString(tc.getArguments()));
                    }
                }
            }

            if (availableTools != null && !availableTools.isEmpty()) {
                ArrayNode toolsArray = requestBody.putArray("tools");
                for (DevOpsTool tool : availableTools) {
                    ObjectNode toolObj = toolsArray.addObject();
                    toolObj.put("type", "function");
                    ObjectNode funcObj = toolObj.putObject("function");
                    funcObj.put("name", tool.getName());
                    funcObj.put("description", tool.getDescription());

                    ObjectNode parameters = funcObj.putObject("parameters");
                    parameters.put("type", "object");
                    ObjectNode properties = parameters.putObject("properties");
                    ArrayNode requiredArray = parameters.putArray("required");

                    for (String allowed : tool.getAllowedParameters()) {
                        ObjectNode propObj = properties.putObject(allowed);
                        propObj.put("type", "string");
                        propObj.put("description", "Parameter " + allowed);
                    }
                    for (String req : tool.getRequiredParameters()) {
                        requiredArray.add(req);
                    }
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("Dispatching prompt to OpenAI-compatible API (Model: {} at {})", model, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OpenAI API call failed with status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choice = root.path("choices").get(0);
            JsonNode messageNode = choice.path("message");
            String content = messageNode.path("content").asText("");

            List<LlmToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = messageNode.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tc : toolCallsNode) {
                    String id = tc.path("id").asText();
                    String funcName = tc.path("function").path("name").asText();
                    String rawArgs = tc.path("function").path("arguments").asText("{}");
                    Map<String, Object> args = new HashMap<>();
                    try {
                        args = objectMapper.readValue(rawArgs, Map.class);
                    } catch (Exception ignored) {}
                    toolCalls.add(new LlmToolCall(id, funcName, args));
                }
            }

            if (!toolCalls.isEmpty()) {
                return LlmResponse.withToolCalls(toolCalls, content);
            } else {
                return LlmResponse.withContent(content);
            }

        } catch (Exception ex) {
            log.error("Failed to execute OpenAI chat request", ex);
            throw new RuntimeException("OpenAI LLM error: " + ex.getMessage(), ex);
        }
    }
}
