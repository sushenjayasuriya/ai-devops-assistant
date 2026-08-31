package ai.devops.modules.infrastructure.server.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import ai.devops.modules.infrastructure.server.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servers")
@Tag(name = "Servers", description = "Endpoints for inspecting physical/virtual Linux host infrastructure and OS metrics")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    @Operation(summary = "List servers, optionally filtered by environment ID")
    public ResponseEntity<ApiResponse<List<ServerEntity>>> getServers(@RequestParam(required = false) UUID envId) {
        List<ServerEntity> servers = serverService.getServers(envId);
        return ResponseEntity.ok(ApiResponse.ok(servers));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get server details by ID")
    public ResponseEntity<ApiResponse<ServerEntity>> getServerById(@PathVariable UUID id) {
        ServerEntity server = serverService.getServerById(id);
        return ResponseEntity.ok(ApiResponse.ok(server));
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get live server CPU, Memory, Disk, and Process metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServerLiveMetrics(@PathVariable UUID id) {
        Map<String, Object> metrics = serverService.getServerLiveMetrics(id);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }
}
