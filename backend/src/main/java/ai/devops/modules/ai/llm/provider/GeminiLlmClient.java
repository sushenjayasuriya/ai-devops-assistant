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
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiLlmClient(
            @Value("${app.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-1.5-flash}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
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

    // Testing constructor
    public GeminiLlmClient(String apiKey, String model, String baseUrl, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("mock-key");
    }

    @Override
    public LlmResponse generateChat(List<LlmMessage> messages, List<DevOpsTool> availableTools) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Gemini API Key is not configured. Set GEMINI_API_KEY.");
        }

        try {
            String endpoint = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();

            // 1. Build Contents Array
            ArrayNode contentsArray = requestBody.putArray("contents");
            for (LlmMessage msg : messages) {
                ObjectNode contentObj = contentsArray.addObject();
                String role = switch (msg.getRole()) {
                    case USER -> "user";
                    case ASSISTANT -> "model";
                    case SYSTEM -> "user"; // In Gemini API, system can be passed as user instruction or systemInstruction
                    case TOOL -> "function";
                };
                contentObj.put("role", role.equals("function") ? "function" : role);

                ArrayNode partsArray = contentObj.putArray("parts");

                if (msg.getRole() == LlmMessage.Role.TOOL) {
                    ObjectNode funcResponseObj = partsArray.addObject().putObject("functionResponse");
                    funcResponseObj.put("name", msg.getToolName());
                    ObjectNode responsePayload = funcResponseObj.putObject("response");
                    try {
                        JsonNode parsed = objectMapper.readTree(msg.getContent());
                        responsePayload.set("result", parsed);
                    } catch (Exception e) {
                        responsePayload.put("output", msg.getContent());
                    }
                } else if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        partsArray.addObject().put("text", msg.getContent());
                    }
                    for (LlmToolCall tc : msg.getToolCalls()) {
                        ObjectNode funcCallObj = partsArray.addObject().putObject("functionCall");
                        funcCallObj.put("name", tc.getName());
                        ObjectNode argsObj = funcCallObj.putObject("args");
                        if (tc.getArguments() != null) {
                            for (Map.Entry<String, Object> entry : tc.getArguments().entrySet()) {
                                argsObj.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
                            }
                        }
                    }
                } else {
                    partsArray.addObject().put("text", msg.getContent() != null ? msg.getContent() : "");
                }
            }

            // 2. Build Tool Function Declarations
            if (availableTools != null && !availableTools.isEmpty()) {
                ArrayNode toolsArray = requestBody.putArray("tools");
                ObjectNode toolWrapper = toolsArray.addObject();
                ArrayNode funcDecls = toolWrapper.putArray("function_declarations");

                for (DevOpsTool tool : availableTools) {
                    ObjectNode funcDecl = funcDecls.addObject();
                    funcDecl.put("name", tool.getName());
                    funcDecl.put("description", tool.getDescription());

                    ObjectNode parameters = funcDecl.putObject("parameters");
                    parameters.put("type", "OBJECT");
                    ObjectNode properties = parameters.putObject("properties");
                    ArrayNode requiredArray = parameters.putArray("required");

                    for (String allowed : tool.getAllowedParameters()) {
                        ObjectNode paramProp = properties.putObject(allowed);
                        paramProp.put("type", "STRING");
                        paramProp.put("description", "Parameter for " + allowed);
                    }

                    for (String req : tool.getRequiredParameters()) {
                        requiredArray.add(req);
                    }
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("Dispatching prompt to Google Gemini API (Model: {})", model);
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Gemini API call failed with status: " + response.getStatusCode());
            }

            return parseGeminiResponse(response.getBody());

        } catch (Exception ex) {
            log.error("Failed to execute Gemini API request", ex);
            throw new RuntimeException("Gemini LLM error: " + ex.getMessage(), ex);
        }
    }

    private LlmResponse parseGeminiResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return LlmResponse.withContent("No response generated by Gemini.");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode parts = firstCandidate.path("content").path("parts");

        StringBuilder textBuilder = new StringBuilder();
        List<LlmToolCall> toolCalls = new ArrayList<>();

        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    textBuilder.append(part.get("text").asText());
                }
                if (part.has("functionCall")) {
                    JsonNode fc = part.get("functionCall");
                    String funcName = fc.path("name").asText();
                    Map<String, Object> args = new HashMap<>();
                    JsonNode argsNode = fc.path("args");
                    if (argsNode.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> fields = argsNode.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> entry = fields.next();
                            args.put(entry.getKey(), entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue());
                        }
                    }
                    toolCalls.add(new LlmToolCall(UUID.randomUUID().toString(), funcName, args));
                }
            }
        }

        if (!toolCalls.isEmpty()) {
            return LlmResponse.withToolCalls(toolCalls, textBuilder.toString());
        } else {
            return LlmResponse.withContent(textBuilder.toString());
        }
    }
}
