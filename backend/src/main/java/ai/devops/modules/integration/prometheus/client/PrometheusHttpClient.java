package ai.devops.modules.integration.prometheus.client;

import ai.devops.common.exception.*;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.modules.integration.prometheus.dto.PrometheusTargetsResponse;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class PrometheusHttpClient {

    private static final Logger log = LoggerFactory.getLogger(PrometheusHttpClient.class);

    private final SsrfProtectionValidator ssrfValidator;
    private final RestTemplate restTemplate;

    public PrometheusHttpClient(SsrfProtectionValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public PrometheusQueryResponse executeInstantQuery(String baseUrl, String authHeader, String query, Instant time, int timeoutMs) {
        ssrfValidator.validateEndpointUrl(baseUrl);
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(cleanBaseUrl + "/api/v1/query")
                .queryParam("query", query);
        if (time != null) {
            uriBuilder.queryParam("time", time.getEpochSecond());
        }

        URI uri = uriBuilder.build().encode().toUri();
        log.info("Executing Prometheus Instant Query: {} (URI: {})", query, uri);

        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PrometheusQueryResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, PrometheusQueryResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("PROMETHEUS", "Non-200 response or empty body: " + response.getStatusCode());
            }

            PrometheusQueryResponse body = response.getBody();
            if ("error".equalsIgnoreCase(body.getStatus())) {
                throw new IntegrationInvalidResponseException("PROMETHEUS", "Query error: " + body.getError());
            }

            return body;
        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new IntegrationTimeoutException("PROMETHEUS", "query=" + query, timeoutMs);
            }
            throw new IntegrationUnavailableException("PROMETHEUS", "Failed to connect to Prometheus at " + baseUrl + ": " + ex.getMessage(), ex);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new IntegrationAuthException("PROMETHEUS", "Authentication/Authorization failure: " + ex.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Prometheus HTTP error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Unexpected error executing PromQL: " + ex.getMessage());
        }
    }

    public PrometheusQueryResponse executeRangeQuery(String baseUrl, String authHeader, String query, Instant start, Instant end, String step, int timeoutMs) {
        ssrfValidator.validateEndpointUrl(baseUrl);
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(cleanBaseUrl + "/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", start != null ? start.getEpochSecond() : Instant.now().minusSeconds(3600).getEpochSecond())
                .queryParam("end", end != null ? end.getEpochSecond() : Instant.now().getEpochSecond())
                .queryParam("step", step != null && !step.isBlank() ? step : "15s");

        URI uri = uriBuilder.build().encode().toUri();
        log.info("Executing Prometheus Range Query: {} (start={}, end={}, step={})", query, start, end, step);

        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PrometheusQueryResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, PrometheusQueryResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("PROMETHEUS", "Non-200 response or empty body: " + response.getStatusCode());
            }

            PrometheusQueryResponse body = response.getBody();
            if ("error".equalsIgnoreCase(body.getStatus())) {
                throw new IntegrationInvalidResponseException("PROMETHEUS", "Range query error: " + body.getError());
            }

            return body;
        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new IntegrationTimeoutException("PROMETHEUS", "query_range=" + query, timeoutMs);
            }
            throw new IntegrationUnavailableException("PROMETHEUS", "Failed to connect to Prometheus at " + baseUrl + ": " + ex.getMessage(), ex);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new IntegrationAuthException("PROMETHEUS", "Authentication/Authorization failure: " + ex.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Prometheus HTTP error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Unexpected error executing PromQL range: " + ex.getMessage());
        }
    }

    public PrometheusTargetsResponse getTargets(String baseUrl, String authHeader, int timeoutMs) {
        ssrfValidator.validateEndpointUrl(baseUrl);
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);

        URI uri = URI.create(cleanBaseUrl + "/api/v1/targets");
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PrometheusTargetsResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, PrometheusTargetsResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IntegrationInvalidResponseException("PROMETHEUS", "Non-200 response fetching targets: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new IntegrationTimeoutException("PROMETHEUS", "getTargets", timeoutMs);
            }
            throw new IntegrationUnavailableException("PROMETHEUS", "Failed to connect to Prometheus targets at " + baseUrl + ": " + ex.getMessage(), ex);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new IntegrationAuthException("PROMETHEUS", "Authentication failure fetching targets: " + ex.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Prometheus HTTP error: " + ex.getStatusCode());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationInvalidResponseException("PROMETHEUS", "Unexpected error fetching Prometheus targets: " + ex.getMessage());
        }
    }

    public Map<String, Object> testConnection(String baseUrl, String authHeader) {
        ssrfValidator.validateEndpointUrl(baseUrl);
        String cleanBaseUrl = sanitizeBaseUrl(baseUrl);

        long start = System.currentTimeMillis();
        URI uri = URI.create(cleanBaseUrl + "/-/healthy");
        HttpHeaders headers = createHeaders(authHeader);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            long latency = System.currentTimeMillis() - start;

            boolean isHealthy = response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null && response.getBody().toLowerCase().contains("healthy");

            return Map.of(
                    "status", isHealthy ? "HEALTHY" : "DEGRADED",
                    "connected", isHealthy,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "statusCode", response.getStatusCode().value()
            );
        } catch (ResourceAccessException ex) {
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorCode", "CONNECTION_FAILED",
                    "errorMessage", "Prometheus server is unreachable: " + ex.getMessage()
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

    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return headers;
    }

    private String sanitizeBaseUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
