package ai.devops.modules.ai.llm;

import ai.devops.modules.ai.llm.provider.GeminiLlmClient;
import ai.devops.modules.ai.llm.provider.MockLlmClient;
import ai.devops.modules.ai.llm.provider.OllamaLlmClient;
import ai.devops.modules.ai.llm.provider.OpenAiCompatibleLlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LlmClientFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmClientFactory.class);

    private final String configuredProvider;
    private final Map<String, LlmClient> clients = new HashMap<>();
    private final MockLlmClient mockClient;

    public LlmClientFactory(
            @Value("${app.ai.provider:gemini}") String configuredProvider,
            GeminiLlmClient geminiClient,
            OllamaLlmClient ollamaClient,
            OpenAiCompatibleLlmClient openAiClient,
            MockLlmClient mockClient) {
        this.configuredProvider = configuredProvider;
        this.mockClient = mockClient;

        clients.put("gemini", geminiClient);
        clients.put("google", geminiClient);
        clients.put("ollama", ollamaClient);
        clients.put("openai", openAiClient);
        clients.put("mock", mockClient);

        log.info("Initialized LLM Client Factory. Default provider: [{}]", configuredProvider);
    }

    public LlmClient getClient() {
        return getClient(configuredProvider);
    }

    public LlmClient getClient(String providerOverride) {
        String provider = (providerOverride != null && !providerOverride.isBlank())
                ? providerOverride.toLowerCase().trim()
                : configuredProvider.toLowerCase().trim();

        LlmClient client = clients.get(provider);
        if (client != null && client.isConfigured()) {
            return client;
        }

        // If requested provider is not configured, check Gemini
        LlmClient gemini = clients.get("gemini");
        if (gemini != null && gemini.isConfigured()) {
            return gemini;
        }

        // Fallback to Mock
        log.warn("Configured LLM provider [{}] is not configured with valid API keys/endpoints. Falling back to Mock LLM engine.", provider);
        return mockClient;
    }
}
