package ai.devops.modules.audit.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.common.response.PageResponse;
import ai.devops.modules.audit.entity.AuditLogEntity;
import ai.devops.modules.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Logs", description = "Endpoints for querying immutable security and infrastructure audit logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Get paginated audit logs with optional environment filter")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogEntity>>> getAuditLogs(
            @RequestParam(required = false) String env,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogEntity> logPage = auditService.getAuditLogs(env, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(logPage)));
    }
}
