package ai.devops.modules.ai.llm;

import java.util.ArrayList;
import java.util.List;

public class LlmResponse {

    private String content;
    private List<LlmToolCall> toolCalls = new ArrayList<>();
    private String finishReason; // "STOP", "TOOL_CALLS", "LENGTH"
    private int promptTokens;
    private int completionTokens;

    public LlmResponse() {}

    public LlmResponse(String content) {
        this.content = content;
        this.finishReason = "STOP";
    }

    public static LlmResponse withContent(String content) {
        return new LlmResponse(content);
    }

    public static LlmResponse withToolCalls(List<LlmToolCall> toolCalls, String thoughtContent) {
        LlmResponse res = new LlmResponse();
        res.setContent(thoughtContent);
        res.setToolCalls(toolCalls != null ? toolCalls : new ArrayList<>());
        res.setFinishReason("TOOL_CALLS");
        return res;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<LlmToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }
}
