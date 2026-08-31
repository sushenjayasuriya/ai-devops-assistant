package ai.devops.modules.ai.tools;

import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.security.rbac.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {

    @Mock
    private AuditService auditService;

    @Mock
    private DevOpsTool mockTool;

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        lenient().when(mockTool.getName()).thenReturn("test_tool");
        lenient().when(mockTool.getDescription()).thenReturn("A test tool");
        lenient().when(mockTool.getParameterSchema()).thenReturn(Map.of("param1", "string"));
        lenient().when(mockTool.getRiskLevel()).thenReturn(RiskLevel.LOW_RISK);
        lenient().when(mockTool.getRequiredRole()).thenReturn(Role.VIEWER);
        lenient().when(mockTool.requiresProductionApproval()).thenReturn(false);

        toolRegistry = new ToolRegistry(List.of(mockTool), auditService);
    }

    @Test
    @DisplayName("Should successfully discover and register tools")
    void testToolDiscovery() {
        assertEquals(1, toolRegistry.getAvailableTools().size());
        assertEquals("test_tool", toolRegistry.getTool("test_tool").getName());
    }

    @Test
    @DisplayName("Should execute tool and record audit entry")
    void testToolExecution() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@devops.ai", "pass", List.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
        );

        when(mockTool.execute(any())).thenReturn(ToolExecutionResult.ok("test_tool", "result-payload"));

        ToolExecutionResult result = toolRegistry.executeTool("test_tool", Map.of("environment", "DEVELOPMENT"));

        assertTrue(result.isSuccess());
        assertEquals("result-payload", result.getData());
        verify(auditService, times(1)).recordAudit(
                eq("AI_TOOL_EXECUTE:test_tool"),
                eq("TOOL"),
                eq("test_tool"),
                eq("DEVELOPMENT"),
                eq(RiskLevel.LOW_RISK),
                anyString(),
                eq("SUCCESS"),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Should block tool execution if user lacks required role")
    void testToolExecutionUnauthorized() {
        when(mockTool.getRequiredRole()).thenReturn(Role.ADMIN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@devops.ai", "pass", List.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
        );

        assertThrows(UnauthorizedActionException.class, () ->
                toolRegistry.executeTool("test_tool", Map.of())
        );
    }
}
