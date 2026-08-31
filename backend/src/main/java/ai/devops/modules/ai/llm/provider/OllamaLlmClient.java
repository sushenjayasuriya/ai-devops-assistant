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
public class OllamaLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OllamaLlmClient(
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:llama3.2}") String model,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    public OllamaLlmClient(String baseUrl, String model, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public LlmResponse generateChat(List<LlmMessage> messages, List<DevOpsTool> availableTools) {
        try {
            String endpoint = baseUrl + "/api/chat";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("stream", false);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (LlmMessage msg : messages) {
                ObjectNode msgObj = messagesArray.addObject();
                msgObj.put("role", msg.getRole().name().toLowerCase());
                msgObj.put("content", msg.getContent() != null ? msg.getContent() : "");
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
                        propObj.put("description", "Argument " + allowed);
                    }
                    for (String req : tool.getRequiredParameters()) {
                        requiredArray.add(req);
                    }
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("Dispatching prompt to local Ollama (Model: {} at {})", model, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Ollama API failed with status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messageNode = root.path("message");
            String content = messageNode.path("content").asText("");

            List<LlmToolCall> toolCalls = new ArrayList<>();
            JsonNode toolCallsNode = messageNode.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tc : toolCallsNode) {
                    String funcName = tc.path("function").path("name").asText();
                    Map<String, Object> args = new HashMap<>();
                    JsonNode argsNode = tc.path("function").path("arguments");
                    if (argsNode.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> fields = argsNode.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> entry = fields.next();
                            args.put(entry.getKey(), entry.getValue().asText());
                        }
                    }
                    toolCalls.add(new LlmToolCall(UUID.randomUUID().toString(), funcName, args));
                }
            }

            if (!toolCalls.isEmpty()) {
                return LlmResponse.withToolCalls(toolCalls, content);
            } else {
                return LlmResponse.withContent(content);
            }

        } catch (Exception ex) {
            log.error("Failed to execute Ollama chat", ex);
            throw new RuntimeException("Ollama LLM error: " + ex.getMessage(), ex);
        }
    }
}
