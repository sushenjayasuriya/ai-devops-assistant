package ai.devops.modules.ai.tools;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.security.rbac.Role;
import ai.devops.security.rbac.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, DevOpsTool> toolMap = new HashMap<>();
    private final AuditService auditService;

    public ToolRegistry(List<DevOpsTool> tools, AuditService auditService) {
        this.auditService = auditService;
        for (DevOpsTool tool : tools) {
            toolMap.put(tool.getName().toLowerCase(), tool);
            log.info("Registered AI DevOps Tool: [{}] (Risk: {}, Required Role: {})",
                    tool.getName(), tool.getRiskLevel(), tool.getRequiredRole());
        }
    }

    public DevOpsTool getTool(String name) {
        DevOpsTool tool = toolMap.get(name.toLowerCase());
        if (tool == null) {
            throw new ResourceNotFoundException("DevOpsTool", name);
        }
        return tool;
    }

    public List<ToolMetadata> getAvailableTools() {
        return toolMap.values().stream()
                .map(ToolMetadata::from)
                .toList();
    }

    public ToolExecutionResult executeTool(String toolName, Map<String, Object> parameters) {
        DevOpsTool tool = getTool(toolName);

        // RBAC Check
        if (tool.getRequiredRole() == Role.ADMIN && !SecurityUtils.isAdmin()) {
            throw new UnauthorizedActionException(toolName, Role.ADMIN.name());
        } else if (tool.getRequiredRole() == Role.DEVOPS_ENGINEER && !SecurityUtils.isDevopsEngineer()) {
            throw new UnauthorizedActionException(toolName, Role.DEVOPS_ENGINEER.name());
        }

        String envName = parameters != null && parameters.containsKey("environment") ?
                String.valueOf(parameters.get("environment")) : "UNKNOWN";

        try {
            ToolExecutionResult result = tool.execute(parameters != null ? parameters : Map.of());

            auditService.recordAudit(
                    "AI_TOOL_EXECUTE:" + tool.getName(),
                    "TOOL",
                    tool.getName(),
                    envName,
                    tool.getRiskLevel(),
                    parameters != null ? parameters.toString() : "{}",
                    result.isSuccess() ? "SUCCESS" : "FAILURE",
                    result.getError(),
                    null
            );

            return result;
        } catch (Exception ex) {
            log.error("Tool execution failed: {}", toolName, ex);
            auditService.recordAudit(
                    "AI_TOOL_EXECUTE:" + tool.getName(),
                    "TOOL",
                    tool.getName(),
                    envName,
                    tool.getRiskLevel(),
                    parameters != null ? parameters.toString() : "{}",
                    "FAILURE",
                    ex.getMessage(),
                    null
            );
            return ToolExecutionResult.error(toolName, ex.getMessage());
        }
    }
}
