package ai.devops.modules.approval.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.approval.dto.ResolveApprovalRequest;
import ai.devops.modules.approval.entity.ApprovalRequestEntity;
import ai.devops.modules.approval.service.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approvals", description = "Endpoints for managing and resolving Human-in-the-Loop remediation approvals")
public class ApprovalController {

    private final ApprovalWorkflowService approvalService;

    public ApprovalController(ApprovalWorkflowService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @Operation(summary = "Get pending approval requests, or filter by environment")
    public ResponseEntity<ApiResponse<List<ApprovalRequestEntity>>> getApprovals(@RequestParam(required = false) UUID envId) {
        List<ApprovalRequestEntity> approvals;
        if (envId != null) {
            approvals = approvalService.getApprovalsByEnvironment(envId);
        } else {
            approvals = approvalService.getPendingApprovals();
        }
        return ResponseEntity.ok(ApiResponse.ok(approvals));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Approve or reject a remediation action approval request")
    public ResponseEntity<ApiResponse<ApprovalRequestEntity>> resolveApproval(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveApprovalRequest request) {
        ApprovalRequestEntity resolved = approvalService.resolveApproval(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Approval resolved successfully", resolved));
    }
}
