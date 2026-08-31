package ai.devops.modules.infrastructure.container.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.infrastructure.container.entity.ContainerEntity;
import ai.devops.modules.infrastructure.container.service.ContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/containers")
@Tag(name = "Containers", description = "Endpoints for inspecting Docker containers, streaming logs, and executing lifecycle actions")
public class ContainerController {

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService) {
        this.containerService = containerService;
    }

    @GetMapping
    @Operation(summary = "List containers, optionally filtered by environment ID")
    public ResponseEntity<ApiResponse<List<ContainerEntity>>> getContainers(@RequestParam(required = false) UUID envId) {
        List<ContainerEntity> containers = containerService.getContainers(envId);
        return ResponseEntity.ok(ApiResponse.ok(containers));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get container by ID")
    public ResponseEntity<ApiResponse<ContainerEntity>> getContainerById(@PathVariable UUID id) {
        ContainerEntity container = containerService.getContainerById(id);
        return ResponseEntity.ok(ApiResponse.ok(container));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get container logs")
    public ResponseEntity<ApiResponse<List<String>>> getContainerLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "100") int tail) {
        List<String> logs = containerService.getContainerLogs(id, tail);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @PostMapping("/{id}/actions")
    @Operation(summary = "Execute lifecycle action on container (restart, stop, start)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeAction(
            @PathVariable UUID id,
            @RequestParam String action,
            @RequestParam(defaultValue = "false") boolean approved) {
        Map<String, Object> result = containerService.executeContainerAction(id, action, approved);
        return ResponseEntity.ok(ApiResponse.ok("Container action executed successfully", result));
    }
}
