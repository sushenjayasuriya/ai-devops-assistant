package ai.devops.modules.integration.prometheus;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.prometheus.client.PrometheusHttpClient;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.modules.integration.prometheus.dto.PrometheusTargetsResponse;
import ai.devops.security.encryption.SecretCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class PrometheusIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(PrometheusIntegration.class);

    private final PrometheusHttpClient prometheusClient;
    private final SecretCryptoService cryptoService;

    public PrometheusIntegration(PrometheusHttpClient prometheusClient, SecretCryptoService cryptoService) {
        this.prometheusClient = prometheusClient;
        this.cryptoService = cryptoService;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.PROMETHEUS;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        String authHeader = resolveAuthHeader(configEncrypted);
        Map<String, Object> result = prometheusClient.testConnection(endpointUrl, authHeader);
        return Boolean.TRUE.equals(result.get("connected"));
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        String authHeader = resolveAuthHeader(configEncrypted);
        return prometheusClient.testConnection(endpointUrl, authHeader);
    }

    public PrometheusQueryResponse executePromQl(String query, String endpointUrl, String configEncrypted, Instant time, int timeoutMs) {
        String authHeader = resolveAuthHeader(configEncrypted);
        return prometheusClient.executeInstantQuery(endpointUrl, authHeader, query, time, timeoutMs > 0 ? timeoutMs : 5000);
    }

    public PrometheusQueryResponse executeRangeQuery(String query, String endpointUrl, String configEncrypted, Instant start, Instant end, String step, int timeoutMs) {
        String authHeader = resolveAuthHeader(configEncrypted);
        return prometheusClient.executeRangeQuery(endpointUrl, authHeader, query, start, end, step, timeoutMs > 0 ? timeoutMs : 10000);
    }

    public PrometheusTargetsResponse getTargets(String endpointUrl, String configEncrypted, int timeoutMs) {
        String authHeader = resolveAuthHeader(configEncrypted);
        return prometheusClient.getTargets(endpointUrl, authHeader, timeoutMs > 0 ? timeoutMs : 5000);
    }

    private String resolveAuthHeader(String configEncrypted) {
        if (configEncrypted == null || configEncrypted.isBlank()) {
            return null;
        }
        try {
            String decrypted = cryptoService.decrypt(configEncrypted);
            if (decrypted != null && !decrypted.isBlank()) {
                if (decrypted.startsWith("Bearer ") || decrypted.startsWith("Basic ")) {
                    return decrypted;
                }
                return "Bearer " + decrypted;
            }
        } catch (Exception ex) {
            log.warn("Failed to decrypt Prometheus authentication credentials: {}", ex.getMessage());
        }
        return null;
    }
}
