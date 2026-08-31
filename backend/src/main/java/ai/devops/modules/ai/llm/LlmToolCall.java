package ai.devops.modules.ai.llm;

import java.util.HashMap;
import java.util.Map;

public class LlmToolCall {

    private String id;
    private String name;
    private Map<String, Object> arguments = new HashMap<>();

    public LlmToolCall() {}

    public LlmToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments != null ? arguments : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
