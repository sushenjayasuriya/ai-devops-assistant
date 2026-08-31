package ai.devops.modules.integration.docker.client;

import ai.devops.common.exception.*;
import ai.devops.modules.integration.docker.dto.DockerContainerDetails;
import ai.devops.modules.integration.docker.dto.DockerContainerStats;
import ai.devops.modules.integration.docker.dto.DockerContainerSummary;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class DockerEngineClient {

    private static final Logger log = LoggerFactory.getLogger(DockerEngineClient.class);

    private final SsrfProtectionValidator ssrfValidator;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String defaultDockerHost;

    public DockerEngineClient(
            SsrfProtectionValidator ssrfValidator,
            ObjectMapper objectMapper,
            @Value("${app.integrations.docker.default-host:tcp://localhost:2375}") String defaultDockerHost) {
        this.ssrfValidator = ssrfValidator;
        this.objectMapper = objectMapper;
        this.defaultDockerHost = defaultDockerHost;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(factory);
    }

    public List<DockerContainerSummary> listContainers(String endpointUrl, boolean all) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/json" + (all ? "?all=true" : "");
        log.info("Querying Docker containers list: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to list containers: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            List<DockerContainerSummary> result = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode item : root) {
                    List<String> names = new ArrayList<>();
                    if (item.has("Names")) {
                        item.get("Names").forEach(n -> names.add(n.asText().replaceFirst("^/", "")));
                    }

                    DockerContainerSummary summary = new DockerContainerSummary(
                            item.has("Id") ? item.get("Id").asText() : "",
                            names,
                            item.has("Image") ? item.get("Image").asText() : "",
                            item.has("State") ? item.get("State").asText() : "",
                            item.has("Status") ? item.get("Status").asText() : ""
                    );
                    if (item.has("Created")) {
                        summary.setCreated(item.get("Created").asLong());
                    }
                    if (item.has("Command")) {
                        summary.setCommand(item.get("Command").asText());
                    }
                    result.add(summary);
                }
            }
            return result;
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable at " + endpointUrl + ": " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to parse Docker containers list: " + ex.getMessage());
        }
    }

    public DockerContainerDetails inspectContainer(String endpointUrl, String containerId) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/json";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("DockerContainer", containerId);
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to inspect container: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            DockerContainerDetails details = new DockerContainerDetails();
            details.setId(root.path("Id").asText());
            details.setName(root.path("Name").asText().replaceFirst("^/", ""));
            details.setImage(root.path("Config").path("Image").asText());

            JsonNode state = root.path("State");
            details.setState(state.path("Status").asText());
            details.setRunning(state.path("Running").asBoolean());
            details.setRestarting(state.path("Restarting").asBoolean());
            details.setPaused(state.path("Paused").asBoolean());
            details.setExitCode(state.path("ExitCode").asInt());
            details.setError(state.path("Error").asText());
            details.setStartedAt(state.path("StartedAt").asText());
            details.setFinishedAt(state.path("FinishedAt").asText());
            details.setRestartCount(root.path("RestartCount").asInt());

            return details;
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("DockerContainer", containerId);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to inspect container: " + ex.getMessage());
        }
    }

    public DockerContainerStats getContainerStats(String endpointUrl, String containerId) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/stats?stream=false";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to get container stats: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String name = root.path("name").asText().replaceFirst("^/", "");

            // Calculate CPU percentage
            long cpuDelta = root.path("cpu_stats").path("cpu_usage").path("total_usage").asLong()
                    - root.path("precpu_stats").path("cpu_usage").path("total_usage").asLong();
            long systemDelta = root.path("cpu_stats").path("system_cpu_usage").asLong()
                    - root.path("precpu_stats").path("system_cpu_usage").asLong();
            int onlineCpus = root.path("cpu_stats").path("online_cpus").asInt(1);

            double cpuPercent = 0.0;
            if (systemDelta > 0 && cpuDelta > 0) {
                cpuPercent = ((double) cpuDelta / (double) systemDelta) * onlineCpus * 100.0;
            }

            long memUsage = root.path("memory_stats").path("usage").asLong();
            long memLimit = root.path("memory_stats").path("limit").asLong(1);
            double memPercent = memLimit > 0 ? ((double) memUsage / (double) memLimit) * 100.0 : 0.0;

            long rxBytes = 0;
            long txBytes = 0;
            JsonNode networks = root.path("networks");
            if (networks.isObject()) {
                for (JsonNode iface : networks) {
                    rxBytes += iface.path("rx_bytes").asLong();
                    txBytes += iface.path("tx_bytes").asLong();
                }
            }

            int pids = root.path("pids_stats").path("current").asInt(0);

            return new DockerContainerStats(
                    containerId,
                    name,
                    Math.round(cpuPercent * 100.0) / 100.0,
                    memUsage,
                    memLimit,
                    Math.round(memPercent * 100.0) / 100.0,
                    rxBytes,
                    txBytes,
                    pids
            );
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to parse container stats: " + ex.getMessage());
        }
    }

    public List<String> getContainerLogs(String endpointUrl, String containerId, int tail) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/logs?stdout=true&stderr=true&tail=" + Math.max(1, tail);
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            // Docker multiplexed log stream has 8-byte header per frame; split on newlines and clean non-printable chars
            String rawLogs = response.getBody();
            String[] lines = rawLogs.split("\r?\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank()) {
                    // Strip binary docker header bytes if present
                    String sanitized = line.replaceAll("^[\\x00-\\x1F]{1,8}", "").trim();
                    if (!sanitized.isBlank()) {
                        cleaned.add(sanitized);
                    }
                }
            }
            return cleaned;
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to read container logs: " + ex.getMessage());
        }
    }

    public void restartContainer(String endpointUrl, String containerId) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/restart?t=10";
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to restart container: HTTP " + response.getStatusCode());
            }
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to restart container: " + ex.getMessage());
        }
    }

    public void stopContainer(String endpointUrl, String containerId) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/stop?t=10";
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to stop container: HTTP " + response.getStatusCode());
            }
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to stop container: " + ex.getMessage());
        }
    }

    public void startContainer(String endpointUrl, String containerId) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        String url = baseHttpUrl + "/containers/" + containerId + "/start";
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.NO_CONTENT) {
                throw new IntegrationInvalidResponseException("DOCKER", "Failed to start container: HTTP " + response.getStatusCode());
            }
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException("DOCKER", "Docker daemon is unreachable: " + ex.getMessage(), ex);
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("DOCKER", "Failed to start container: " + ex.getMessage());
        }
    }

    public Map<String, Object> testConnection(String endpointUrl) {
        String baseHttpUrl = toHttpUrl(endpointUrl);
        ssrfValidator.validateEndpointUrl(baseHttpUrl);

        long start = System.currentTimeMillis();
        String url = baseHttpUrl + "/version";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            long latency = System.currentTimeMillis() - start;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String version = root.path("Version").asText("unknown");
                String apiVersion = root.path("ApiVersion").asText("unknown");
                String os = root.path("Os").asText("unknown");

                return Map.of(
                        "status", "HEALTHY",
                        "connected", true,
                        "latencyMs", latency,
                        "version", version,
                        "apiVersion", apiVersion,
                        "os", os,
                        "checkedAt", Instant.now().toString()
                );
            }
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorMessage", "Non-200 status code: " + response.getStatusCode()
            );
        } catch (ResourceAccessException ex) {
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorCode", "CONNECTION_FAILED",
                    "errorMessage", "Docker daemon is unreachable at " + endpointUrl + ": " + ex.getMessage()
            );
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorCode", "HTTP_ERROR",
                    "errorMessage", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
            );
        }
    }

    private String toHttpUrl(String endpoint) {
        String ep = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : defaultDockerHost;
        if (ep.startsWith("tcp://")) {
            return "http://" + ep.substring(6);
        }
        if (ep.startsWith("http://") || ep.startsWith("https://")) {
            return ep.replaceAll("/+$", "");
        }
        // Local unix socket fallback (or proxy bridge)
        return "http://localhost:2375";
    }
}
