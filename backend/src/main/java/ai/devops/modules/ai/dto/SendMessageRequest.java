package ai.devops.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class SendMessageRequest {

    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;

    private UUID environmentId;

    public SendMessageRequest() {}

    public SendMessageRequest(String prompt, UUID environmentId) {
        this.prompt = prompt;
        this.environmentId = environmentId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }
}
