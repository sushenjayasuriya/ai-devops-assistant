package ai.devops.modules.environment.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/environments")
@Tag(name = "Environments", description = "Endpoints for managing and querying environments (DEV, STAGING, PROD)")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping
    @Operation(summary = "Get all configured environments")
    public ResponseEntity<ApiResponse<List<EnvironmentEntity>>> getAllEnvironments() {
        List<EnvironmentEntity> environments = environmentService.getAllEnvironments();
        return ResponseEntity.ok(ApiResponse.ok(environments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get environment details by ID")
    public ResponseEntity<ApiResponse<EnvironmentEntity>> getEnvironmentById(@PathVariable UUID id) {
        EnvironmentEntity environment = environmentService.getEnvironmentById(id);
        return ResponseEntity.ok(ApiResponse.ok(environment));
    }
}
