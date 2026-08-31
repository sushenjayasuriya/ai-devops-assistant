package ai.devops.modules.ai.llm;

import ai.devops.modules.ai.tools.DevOpsTool;
import java.util.List;

public interface LlmClient {

    String getProviderName();

    boolean isConfigured();

    LlmResponse generateChat(List<LlmMessage> messages, List<DevOpsTool> availableTools);
}
