package ai.devops.modules.integration.core.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.security.encryption.SecretCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final EnvironmentRepository environmentRepository;
    private final SecretCryptoService cryptoService;
    private final Map<IntegrationType, InfrastructureIntegration> integrationAdapters = new HashMap<>();

    public IntegrationService(
            IntegrationRepository integrationRepository,
            EnvironmentRepository environmentRepository,
            SecretCryptoService cryptoService,
            List<InfrastructureIntegration> adapters) {
        this.integrationRepository = integrationRepository;
        this.environmentRepository = environmentRepository;
        this.cryptoService = cryptoService;
        for (InfrastructureIntegration adapter : adapters) {
            integrationAdapters.put(adapter.getType(), adapter);
        }
    }

    @Transactional(readOnly = true)
    public List<IntegrationEntity> getIntegrations(UUID environmentId) {
        if (environmentId != null) {
            return integrationRepository.findByEnvironmentId(environmentId);
        }
        return integrationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public IntegrationEntity getIntegrationById(UUID id) {
        return integrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Integration", id));
    }

    @Transactional
    public Map<String, Object> testIntegrationConnection(UUID id) {
        IntegrationEntity integration = getIntegrationById(id);
        InfrastructureIntegration adapter = integrationAdapters.get(integration.getType());

        boolean isConnected = false;
        if (adapter != null) {
            String decryptedConfig = cryptoService.decrypt(integration.getConfigEncrypted());
            isConnected = adapter.testConnection(integration.getEndpointUrl(), decryptedConfig);
        }

        integration.setHealthStatus(isConnected ? "HEALTHY" : "UNHEALTHY");
        integration.setLastSyncedAt(Instant.now());
        integrationRepository.save(integration);

        Map<String, Object> response = new HashMap<>();
        response.put("integrationId", integration.getId());
        response.put("name", integration.getName());
        response.put("type", integration.getType());
        response.put("healthStatus", integration.getHealthStatus());
        response.put("connected", isConnected);
        response.put("testedAt", Instant.now());
        return response;
    }
}
