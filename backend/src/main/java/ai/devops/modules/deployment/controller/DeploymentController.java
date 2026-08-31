package ai.devops.modules.deployment.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deployments")
@Tag(name = "Deployments", description = "Endpoints for tracking CI/CD build deployments, changelogs, and release correlation")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    @Operation(summary = "List recent deployments, optionally filtered by environment ID")
    public ResponseEntity<ApiResponse<List<DeploymentEntity>>> getDeployments(@RequestParam(required = false) UUID envId) {
        List<DeploymentEntity> deployments = deploymentService.getDeployments(envId);
        return ResponseEntity.ok(ApiResponse.ok(deployments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deployment details by ID")
    public ResponseEntity<ApiResponse<DeploymentEntity>> getDeploymentById(@PathVariable UUID id) {
        DeploymentEntity deployment = deploymentService.getDeploymentById(id);
        return ResponseEntity.ok(ApiResponse.ok(deployment));
    }
}
