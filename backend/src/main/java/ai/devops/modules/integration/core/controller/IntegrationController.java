package ai.devops.modules.integration.core.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.service.IntegrationService;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Integrations & Metrics", description = "Endpoints for managing infrastructure connectors and executing safe Prometheus telemetry queries")
public class IntegrationController {

    private final IntegrationService integrationService;
    private final PrometheusIntegration prometheusIntegration;

    public IntegrationController(IntegrationService integrationService, PrometheusIntegration prometheusIntegration) {
        this.integrationService = integrationService;
        this.prometheusIntegration = prometheusIntegration;
    }

    @GetMapping("/integrations")
    @Operation(summary = "List integrations, optionally filtered by environment ID")
    public ResponseEntity<ApiResponse<List<IntegrationEntity>>> getIntegrations(@RequestParam(required = false) UUID envId) {
        List<IntegrationEntity> integrations = integrationService.getIntegrations(envId);
        return ResponseEntity.ok(ApiResponse.ok(integrations));
    }

    @PostMapping("/integrations/{id}/test-connection")
    @Operation(summary = "Test connectivity to external integration target")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(@PathVariable UUID id) {
        Map<String, Object> result = integrationService.testIntegrationConnection(id);
        return ResponseEntity.ok(ApiResponse.ok("Connection test completed", result));
    }

    @GetMapping("/metrics/prometheus/query")
    @Operation(summary = "Execute safe PromQL query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryPrometheus(
            @RequestParam String query,
            @RequestParam(defaultValue = "http://localhost:9090") String endpointUrl) {
        Map<String, Object> queryResult = prometheusIntegration.executePromQl(query, endpointUrl);
        return ResponseEntity.ok(ApiResponse.ok(queryResult));
    }

    @GetMapping("/metrics/prometheus/targets")
    @Operation(summary = "Get Prometheus scrape target health")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPrometheusTargets(
            @RequestParam(defaultValue = "http://localhost:9090") String endpointUrl) {
        List<Map<String, Object>> targets = prometheusIntegration.getTargets(endpointUrl);
        return ResponseEntity.ok(ApiResponse.ok(targets));
    }
}
