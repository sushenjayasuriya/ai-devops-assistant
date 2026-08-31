package ai.devops.modules.integration.core.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.common.model.RiskLevel;
import ai.devops.modules.audit.service.AuditService;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.core.dto.CreateIntegrationRequest;
import ai.devops.modules.integration.core.dto.IntegrationResponseDto;
import ai.devops.modules.integration.core.dto.TestConnectionResult;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import ai.devops.modules.integration.core.repository.IntegrationRepository;
import ai.devops.modules.integration.docker.DockerIntegration;
import ai.devops.modules.integration.docker.dto.DockerContainerDetails;
import ai.devops.modules.integration.docker.dto.DockerContainerStats;
import ai.devops.modules.integration.docker.dto.DockerContainerSummary;
import ai.devops.modules.integration.kubernetes.KubernetesIntegration;
import ai.devops.modules.integration.kubernetes.dto.K8sDeploymentDto;
import ai.devops.modules.integration.kubernetes.dto.K8sNamespaceDto;
import ai.devops.modules.integration.kubernetes.dto.K8sPodDto;
import ai.devops.modules.integration.kubernetes.dto.K8sServiceDto;
import ai.devops.modules.integration.linux.LinuxServerIntegration;
import ai.devops.modules.integration.linux.model.LinuxCommand;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import ai.devops.modules.integration.prometheus.PrometheusIntegration;
import ai.devops.modules.integration.prometheus.dto.PrometheusQueryResponse;
import ai.devops.modules.integration.prometheus.dto.PrometheusTargetsResponse;
import ai.devops.security.encryption.SecretCryptoService;
import ai.devops.security.rbac.SecurityUtils;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private final IntegrationRepository integrationRepository;
    private final EnvironmentRepository environmentRepository;
    private final SecretCryptoService cryptoService;
    private final SsrfProtectionValidator ssrfValidator;
    private final AuditService auditService;

    private final PrometheusIntegration prometheusIntegration;
    private final DockerIntegration dockerIntegration;
    private final LinuxServerIntegration linuxIntegration;
    private final KubernetesIntegration kubernetesIntegration;

    public IntegrationService(
            IntegrationRepository integrationRepository,
            EnvironmentRepository environmentRepository,
            SecretCryptoService cryptoService,
            SsrfProtectionValidator ssrfValidator,
            AuditService auditService,
            PrometheusIntegration prometheusIntegration,
            DockerIntegration dockerIntegration,
            LinuxServerIntegration linuxIntegration,
            KubernetesIntegration kubernetesIntegration) {
        this.integrationRepository = integrationRepository;
        this.environmentRepository = environmentRepository;
        this.cryptoService = cryptoService;
        this.ssrfValidator = ssrfValidator;
        this.auditService = auditService;
        this.prometheusIntegration = prometheusIntegration;
        this.dockerIntegration = dockerIntegration;
        this.linuxIntegration = linuxIntegration;
        this.kubernetesIntegration = kubernetesIntegration;
    }

    @Transactional(readOnly = true)
    public List<IntegrationResponseDto> getIntegrations(UUID environmentId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }

        List<IntegrationEntity> entities = (environmentId != null) ?
                integrationRepository.findByOrganizationIdAndEnvironmentId(orgId, environmentId) :
                integrationRepository.findByOrganizationId(orgId);

        return entities.stream().map(IntegrationResponseDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public IntegrationEntity getIntegrationEntityById(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ResourceNotFoundException("Integration", id);
        }

        return integrationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration", id));
    }

    @Transactional(readOnly = true)
    public IntegrationResponseDto getIntegrationById(UUID id) {
        return IntegrationResponseDto.fromEntity(getIntegrationEntityById(id));
    }

    @Transactional
    public IntegrationResponseDto createIntegration(CreateIntegrationRequest request) {
        if (!SecurityUtils.isDevopsEngineer()) {
            throw new UnauthorizedActionException("Registering integrations requires DEVOPS_ENGINEER or ADMIN role");
        }

        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedActionException("User is not associated with an active organization");
        }

        EnvironmentEntity env = environmentRepository.findByIdAndOrganizationId(request.getEnvironmentId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", request.getEnvironmentId()));

        ssrfValidator.validateEndpointUrl(request.getEndpointUrl());

        IntegrationEntity entity = new IntegrationEntity();
        entity.setEnvironment(env);
        entity.setType(request.getType());
        entity.setName(request.getName().trim());
        entity.setEndpointUrl(request.getEndpointUrl().trim());
        entity.setAuthType(request.getAuthType() != null ? request.getAuthType() : "NONE");
        entity.setTimeoutMs(request.getTimeoutMs() != null ? request.getTimeoutMs() : 5000);
        entity.setEnabled(request.isEnabled());
        entity.setHealthStatus("UNCONFIGURED");

        if (request.getConfigRaw() != null && !request.getConfigRaw().isBlank()) {
            entity.setConfigEncrypted(cryptoService.encrypt(request.getConfigRaw().trim()));
        }

        IntegrationEntity saved = integrationRepository.save(entity);

        auditService.recordAudit(
                "CREATE_INTEGRATION",
                "INTEGRATION",
                saved.getId().toString(),
                env.getName(),
                RiskLevel.MEDIUM_RISK,
                String.format("type=%s, name=%s", saved.getType(), saved.getName()),
                "SUCCESS",
                null,
                null
        );

        return IntegrationResponseDto.fromEntity(saved);
    }

    @Transactional
    public TestConnectionResult testIntegrationConnection(UUID id) {
        IntegrationEntity integration = getIntegrationEntityById(id);
        long start = System.currentTimeMillis();

        boolean isConnected = false;
        String status = "UNHEALTHY";
        String errorCode = null;
        String errorMessage = null;

        try {
            switch (integration.getType()) {
                case PROMETHEUS -> isConnected = prometheusIntegration.testConnection(
                        integration.getEndpointUrl(), integration.getConfigEncrypted());
                case DOCKER -> isConnected = dockerIntegration.testConnection(
                        integration.getEndpointUrl(), integration.getConfigEncrypted());
                case LINUX_SSH -> isConnected = linuxIntegration.testConnection(
                        integration.getEndpointUrl(), integration.getConfigEncrypted());
                case KUBERNETES -> isConnected = kubernetesIntegration.testConnection(
                        integration.getEndpointUrl(), integration.getConfigEncrypted());
                default -> isConnected = true;
            }
            status = isConnected ? "HEALTHY" : "UNHEALTHY";
        } catch (Exception ex) {
            log.error("Integration [{}] connection test failed: {}", integration.getId(), ex.getMessage());
            status = "UNHEALTHY";
            isConnected = false;
            errorCode = "CONNECTION_FAILED";
            errorMessage = ex.getMessage();
        }

        long latency = System.currentTimeMillis() - start;

        integration.setHealthStatus(status);
        integration.setLastSyncedAt(Instant.now());
        integrationRepository.save(integration);

        auditService.recordAudit(
                "TEST_INTEGRATION_CONNECTION",
                "INTEGRATION",
                integration.getId().toString(),
                integration.getEnvironment().getName(),
                RiskLevel.READ_ONLY,
                String.format("type=%s, status=%s", integration.getType(), status),
                isConnected ? "SUCCESS" : "FAILURE",
                errorMessage,
                null
        );

        return new TestConnectionResult(
                integration.getId(),
                integration.getName(),
                integration.getType(),
                isConnected,
                status,
                latency,
                errorCode,
                errorMessage
        );
    }

    // --- PROMETHEUS TELEMETRY PROXY ---
    @Transactional(readOnly = true)
    public PrometheusQueryResponse queryPrometheusInstant(UUID integrationId, String query, Instant time) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.PROMETHEUS) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Prometheus integration");
        }
        return prometheusIntegration.executePromQl(query, integration.getEndpointUrl(), integration.getConfigEncrypted(), time, integration.getTimeoutMs());
    }

    @Transactional(readOnly = true)
    public PrometheusQueryResponse queryPrometheusRange(UUID integrationId, String query, Instant start, Instant end, String step) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.PROMETHEUS) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Prometheus integration");
        }
        return prometheusIntegration.executeRangeQuery(query, integration.getEndpointUrl(), integration.getConfigEncrypted(), start, end, step, integration.getTimeoutMs());
    }

    @Transactional(readOnly = true)
    public PrometheusTargetsResponse getPrometheusTargets(UUID integrationId) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.PROMETHEUS) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Prometheus integration");
        }
        return prometheusIntegration.getTargets(integration.getEndpointUrl(), integration.getConfigEncrypted(), integration.getTimeoutMs());
    }

    // --- DOCKER TELEMETRY PROXY ---
    @Transactional(readOnly = true)
    public List<DockerContainerSummary> listDockerContainers(UUID integrationId, boolean all) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.DOCKER) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Docker integration");
        }
        return dockerIntegration.listContainers(integration.getEndpointUrl(), all);
    }

    @Transactional(readOnly = true)
    public DockerContainerStats getDockerContainerStats(UUID integrationId, String containerId) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.DOCKER) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Docker integration");
        }
        return dockerIntegration.getContainerStats(integration.getEndpointUrl(), containerId);
    }

    @Transactional(readOnly = true)
    public List<String> getDockerContainerLogs(UUID integrationId, String containerId, int tail) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.DOCKER) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Docker integration");
        }
        return dockerIntegration.getContainerLogs(integration.getEndpointUrl(), containerId, tail);
    }

    // --- KUBERNETES TELEMETRY PROXY ---
    @Transactional(readOnly = true)
    public List<K8sPodDto> getKubernetesPods(UUID integrationId, String namespace) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.KUBERNETES) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Kubernetes integration");
        }
        return kubernetesIntegration.getPods(integration.getConfigEncrypted(), namespace);
    }

    @Transactional(readOnly = true)
    public List<K8sDeploymentDto> getKubernetesDeployments(UUID integrationId, String namespace) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.KUBERNETES) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Kubernetes integration");
        }
        return kubernetesIntegration.getDeployments(integration.getConfigEncrypted(), namespace);
    }

    @Transactional(readOnly = true)
    public List<K8sServiceDto> getKubernetesServices(UUID integrationId, String namespace) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.KUBERNETES) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Kubernetes integration");
        }
        return kubernetesIntegration.getServices(integration.getConfigEncrypted(), namespace);
    }

    @Transactional(readOnly = true)
    public List<String> getKubernetesPodLogs(UUID integrationId, String namespace, String podName, String containerName, int tail) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.KUBERNETES) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Kubernetes integration");
        }
        return kubernetesIntegration.getPodLogs(integration.getConfigEncrypted(), namespace, podName, containerName, tail);
    }

    // --- LINUX SSH TELEMETRY PROXY ---
    @Transactional(readOnly = true)
    public LinuxServerTelemetry collectLinuxTelemetry(UUID integrationId) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.LINUX_SSH) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Linux SSH integration");
        }
        return linuxIntegration.collectTelemetry(integration.getEndpointUrl(), integration.getConfigEncrypted(), integration.getTimeoutMs());
    }

    @Transactional(readOnly = true)
    public String executeLinuxTypedCommand(UUID integrationId, LinuxCommand command) {
        IntegrationEntity integration = getIntegrationEntityById(integrationId);
        if (integration.getType() != IntegrationType.LINUX_SSH) {
            throw new IllegalArgumentException("Integration " + integrationId + " is not a Linux SSH integration");
        }
        return linuxIntegration.executeTypedCommand(integration.getEndpointUrl(), integration.getConfigEncrypted(), command, integration.getTimeoutMs());
    }
}
