package ai.devops.modules.ai.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LlmMessage {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    private Role role;
    private String content;
    private List<LlmToolCall> toolCalls = new ArrayList<>();
    private String toolCallId;
    private String toolName;

    public LlmMessage() {}

    public LlmMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content);
    }

    public static LlmMessage assistantWithTools(String content, List<LlmToolCall> toolCalls) {
        LlmMessage msg = new LlmMessage(Role.ASSISTANT, content);
        msg.setToolCalls(toolCalls != null ? toolCalls : new ArrayList<>());
        return msg;
    }

    public static LlmMessage toolResult(String toolCallId, String toolName, String resultJson) {
        LlmMessage msg = new LlmMessage(Role.TOOL, resultJson);
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        return msg;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}
