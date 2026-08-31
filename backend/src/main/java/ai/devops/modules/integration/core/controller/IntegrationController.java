package ai.devops.modules.integration.core.controller;

import ai.devops.common.response.ApiResponse;
import ai.devops.modules.integration.core.dto.CreateIntegrationRequest;
import ai.devops.modules.integration.core.dto.IntegrationResponseDto;
import ai.devops.modules.integration.core.dto.TestConnectionResult;
import ai.devops.modules.integration.core.service.IntegrationService;
import ai.devops.modules.integration.docker.dto.DockerContainerStats;
import ai.devops.modules.integration.docker.dto.DockerContainerSummary;
import ai.devops.modules.integration.kubernetes.dto.K8sDeploymentDto;
import ai.devops.modules.integration.kubernetes.dto.K8sPodDto;
import ai.devops.modules.integration.kubernetes.dto.K8sServiceDto;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.modules.integration.prometheus.dto.PrometheusTargetsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Integrations & Data Plane", description = "Endpoints for managing infrastructure integrations, testing connectivity, and executing safe data plane telemetry queries")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/integrations")
    @Operation(summary = "List integrations for current organization, optionally filtered by environment ID")
    public ResponseEntity<ApiResponse<List<IntegrationResponseDto>>> getIntegrations(@RequestParam(required = false) UUID envId) {
        List<IntegrationResponseDto> integrations = integrationService.getIntegrations(envId);
        return ResponseEntity.ok(ApiResponse.ok(integrations));
    }

    @GetMapping("/integrations/{id}")
    @Operation(summary = "Get integration safe metadata by ID")
    public ResponseEntity<ApiResponse<IntegrationResponseDto>> getIntegrationById(@PathVariable UUID id) {
        IntegrationResponseDto integration = integrationService.getIntegrationById(id);
        return ResponseEntity.ok(ApiResponse.ok(integration));
    }

    @PostMapping("/integrations")
    @Operation(summary = "Register a new infrastructure integration")
    public ResponseEntity<ApiResponse<IntegrationResponseDto>> createIntegration(@Valid @RequestBody CreateIntegrationRequest request) {
        IntegrationResponseDto created = integrationService.createIntegration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Integration created successfully", created));
    }

    @PostMapping("/integrations/{id}/test-connection")
    @Operation(summary = "Test connectivity and latency to external integration target")
    public ResponseEntity<ApiResponse<TestConnectionResult>> testConnection(@PathVariable UUID id) {
        TestConnectionResult result = integrationService.testIntegrationConnection(id);
        return ResponseEntity.ok(ApiResponse.ok("Connection test completed", result));
    }

    // --- PROMETHEUS ENDPOINTS ---
    @GetMapping("/integrations/{id}/prometheus/query")
    @Operation(summary = "Execute safe Instant PromQL query via configured integration")
    public ResponseEntity<ApiResponse<PrometheusQueryResponse>> queryPrometheusInstant(
            @PathVariable UUID id,
            @RequestParam String query,
            @RequestParam(required = false) Instant time) {
        PrometheusQueryResponse response = integrationService.queryPrometheusInstant(id, query, time);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/integrations/{id}/prometheus/query_range")
    @Operation(summary = "Execute safe Range PromQL query via configured integration")
    public ResponseEntity<ApiResponse<PrometheusQueryResponse>> queryPrometheusRange(
            @PathVariable UUID id,
            @RequestParam String query,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "15s") String step) {
        PrometheusQueryResponse response = integrationService.queryPrometheusRange(id, query, start, end, step);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/integrations/{id}/prometheus/targets")
    @Operation(summary = "Get Prometheus scrape target health via configured integration")
    public ResponseEntity<ApiResponse<PrometheusTargetsResponse>> getPrometheusTargets(@PathVariable UUID id) {
        PrometheusTargetsResponse targets = integrationService.getPrometheusTargets(id);
        return ResponseEntity.ok(ApiResponse.ok(targets));
    }

    // --- DOCKER ENDPOINTS ---
    @GetMapping("/integrations/{id}/docker/containers")
    @Operation(summary = "List Docker containers via configured integration")
    public ResponseEntity<ApiResponse<List<DockerContainerSummary>>> listDockerContainers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean all) {
        List<DockerContainerSummary> containers = integrationService.listDockerContainers(id, all);
        return ResponseEntity.ok(ApiResponse.ok(containers));
    }

    @GetMapping("/integrations/{id}/docker/containers/{containerId}/stats")
    @Operation(summary = "Get live container stats (CPU, memory, net, pids) via configured integration")
    public ResponseEntity<ApiResponse<DockerContainerStats>> getDockerContainerStats(
            @PathVariable UUID id,
            @PathVariable String containerId) {
        DockerContainerStats stats = integrationService.getDockerContainerStats(id, containerId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/integrations/{id}/docker/containers/{containerId}/logs")
    @Operation(summary = "Get container logs via configured integration")
    public ResponseEntity<ApiResponse<List<String>>> getDockerContainerLogs(
            @PathVariable UUID id,
            @PathVariable String containerId,
            @RequestParam(defaultValue = "100") int tail) {
        List<String> logs = integrationService.getDockerContainerLogs(id, containerId, tail);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    // --- KUBERNETES ENDPOINTS ---
    @GetMapping("/integrations/{id}/kubernetes/pods")
    @Operation(summary = "Get Kubernetes pods via configured integration")
    public ResponseEntity<ApiResponse<List<K8sPodDto>>> getKubernetesPods(
            @PathVariable UUID id,
            @RequestParam(required = false) String namespace) {
        List<K8sPodDto> pods = integrationService.getKubernetesPods(id, namespace);
        return ResponseEntity.ok(ApiResponse.ok(pods));
    }

    @GetMapping("/integrations/{id}/kubernetes/deployments")
    @Operation(summary = "Get Kubernetes deployments via configured integration")
    public ResponseEntity<ApiResponse<List<K8sDeploymentDto>>> getKubernetesDeployments(
            @PathVariable UUID id,
            @RequestParam(required = false) String namespace) {
        List<K8sDeploymentDto> deployments = integrationService.getKubernetesDeployments(id, namespace);
        return ResponseEntity.ok(ApiResponse.ok(deployments));
    }

    @GetMapping("/integrations/{id}/kubernetes/services")
    @Operation(summary = "Get Kubernetes services via configured integration")
    public ResponseEntity<ApiResponse<List<K8sServiceDto>>> getKubernetesServices(
            @PathVariable UUID id,
            @RequestParam(required = false) String namespace) {
        List<K8sServiceDto> services = integrationService.getKubernetesServices(id, namespace);
        return ResponseEntity.ok(ApiResponse.ok(services));
    }

    @GetMapping("/integrations/{id}/kubernetes/logs")
    @Operation(summary = "Get Kubernetes pod logs via configured integration")
    public ResponseEntity<ApiResponse<List<String>>> getKubernetesPodLogs(
            @PathVariable UUID id,
            @RequestParam String namespace,
            @RequestParam String podName,
            @RequestParam(required = false) String containerName,
            @RequestParam(defaultValue = "100") int tail) {
        List<String> logs = integrationService.getKubernetesPodLogs(id, namespace, podName, containerName, tail);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    // --- LINUX SSH ENDPOINTS ---
    @GetMapping("/integrations/{id}/linux/telemetry")
    @Operation(summary = "Collect Linux server telemetry via configured SSH integration")
    public ResponseEntity<ApiResponse<LinuxServerTelemetry>> getLinuxTelemetry(@PathVariable UUID id) {
        LinuxServerTelemetry telemetry = integrationService.collectLinuxTelemetry(id);
        return ResponseEntity.ok(ApiResponse.ok(telemetry));
    }
}
