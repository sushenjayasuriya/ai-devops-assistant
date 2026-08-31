package ai.devops.modules.ai.tools;

import java.time.Instant;
import java.util.Map;

public class ToolExecutionResult {
    private boolean success;
    private String toolName;
    private Object data;
    private String error;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public ToolExecutionResult() {
        this.timestamp = Instant.now();
    }

    public ToolExecutionResult(boolean success, String toolName, Object data, String error, Map<String, Object> metadata) {
        this.success = success;
        this.toolName = toolName;
        this.data = data;
        this.error = error;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }

    public static ToolExecutionResult ok(String toolName, Object data) {
        return new ToolExecutionResult(true, toolName, data, null, null);
    }

    public static ToolExecutionResult ok(String toolName, Object data, Map<String, Object> metadata) {
        return new ToolExecutionResult(true, toolName, data, null, metadata);
    }

    public static ToolExecutionResult error(String toolName, String error) {
        return new ToolExecutionResult(false, toolName, null, error, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
