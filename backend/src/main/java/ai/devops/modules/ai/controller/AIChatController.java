package ai.devops.modules.ai.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.ai.core.AgentOrchestrator;
import ai.devops.modules.ai.core.FOIRResponse;
import ai.devops.modules.ai.dto.SendMessageRequest;
import ai.devops.modules.ai.entity.AIConversationEntity;
import ai.devops.modules.ai.entity.AIMessageEntity;
import ai.devops.modules.ai.repository.AIConversationRepository;
import ai.devops.modules.ai.repository.AIMessageRepository;
import ai.devops.modules.ai.tools.ToolMetadata;
import ai.devops.modules.ai.tools.ToolRegistry;
import ai.devops.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI DevOps Assistant", description = "Endpoints for interacting with the autonomous ReAct DevOps Assistant and querying available tools")
public class AIChatController {

    private final AgentOrchestrator orchestrator;
    private final ToolRegistry toolRegistry;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;

    public AIChatController(
            AgentOrchestrator orchestrator,
            ToolRegistry toolRegistry,
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository) {
        this.orchestrator = orchestrator;
        this.toolRegistry = toolRegistry;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/tools")
    @Operation(summary = "List all strongly typed infrastructure tools registered in the AI engine")
    public ResponseEntity<ApiResponse<List<ToolMetadata>>> getTools() {
        List<ToolMetadata> tools = toolRegistry.getAvailableTools();
        return ResponseEntity.ok(ApiResponse.ok(tools));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get conversation history for current user")
    public ResponseEntity<ApiResponse<List<AIConversationEntity>>> getConversations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AIConversationEntity> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(conversations));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get all messages in a conversation")
    public ResponseEntity<ApiResponse<List<AIMessageEntity>>> getMessages(@PathVariable UUID id) {
        List<AIMessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @PostMapping("/chat")
    @Operation(summary = "Send prompt to AI Assistant (triggers ReAct telemetry query and FOIR guardrail verification)")
    public ResponseEntity<ApiResponse<FOIRResponse>> chat(
            @RequestParam(required = false) UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        FOIRResponse response = orchestrator.processConversation(conversationId, request.getPrompt(), request.getEnvironmentId());
        return ResponseEntity.ok(ApiResponse.ok("Reasoning cycle complete", response));
    }
}
