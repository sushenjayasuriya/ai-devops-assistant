package ai.devops.modules.integration.kubernetes.client;

import ai.devops.common.exception.*;
import ai.devops.modules.integration.kubernetes.dto.K8sDeploymentDto;
import ai.devops.modules.integration.kubernetes.dto.K8sNamespaceDto;
import ai.devops.modules.integration.kubernetes.dto.K8sPodDto;
import ai.devops.modules.integration.kubernetes.dto.K8sServiceDto;
import ai.devops.security.encryption.SecretCryptoService;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class KubernetesClientService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientService.class);

    private final SecretCryptoService cryptoService;

    public KubernetesClientService(SecretCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public List<K8sNamespaceDto> getNamespaces(String configEncrypted) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            NamespaceList list = client.namespaces().list();
            List<K8sNamespaceDto> result = new ArrayList<>();
            for (Namespace ns : list.getItems()) {
                result.add(new K8sNamespaceDto(
                        ns.getMetadata().getName(),
                        ns.getStatus() != null ? ns.getStatus().getPhase() : "Active",
                        ns.getMetadata().getCreationTimestamp()
                ));
            }
            return result;
        } catch (Exception ex) {
            handleK8sException("getNamespaces", ex);
            return List.of();
        }
    }

    public List<K8sPodDto> getPods(String configEncrypted, String namespace) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            PodList podList = (namespace != null && !namespace.isBlank() && !"all".equalsIgnoreCase(namespace)) ?
                    client.pods().inNamespace(namespace).list() :
                    client.pods().inAnyNamespace().list();

            List<K8sPodDto> result = new ArrayList<>();
            for (Pod pod : podList.getItems()) {
                int totalRestarts = 0;
                List<String> containerNames = new ArrayList<>();
                if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        totalRestarts += (cs.getRestartCount() != null ? cs.getRestartCount() : 0);
                        containerNames.add(cs.getName());
                    }
                }

                String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
                // Check for CrashLoopBackOff or Error in waiting state
                if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        if (cs.getState() != null && cs.getState().getWaiting() != null) {
                            String reason = cs.getState().getWaiting().getReason();
                            if (reason != null && !reason.isBlank()) {
                                phase = reason;
                            }
                        }
                    }
                }

                K8sPodDto dto = new K8sPodDto(
                        pod.getMetadata().getName(),
                        pod.getMetadata().getNamespace(),
                        phase,
                        pod.getSpec() != null ? pod.getSpec().getNodeName() : "unknown",
                        pod.getStatus() != null ? pod.getStatus().getPodIP() : "pending",
                        totalRestarts,
                        pod.getMetadata().getCreationTimestamp(),
                        containerNames
                );
                dto.setLabels(pod.getMetadata().getLabels());
                result.add(dto);
            }
            return result;
        } catch (Exception ex) {
            handleK8sException("getPods", ex);
            return List.of();
        }
    }

    public List<K8sDeploymentDto> getDeployments(String configEncrypted, String namespace) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            var deploymentList = (namespace != null && !namespace.isBlank() && !"all".equalsIgnoreCase(namespace)) ?
                    client.apps().deployments().inNamespace(namespace).list() :
                    client.apps().deployments().inAnyNamespace().list();

            List<K8sDeploymentDto> result = new ArrayList<>();
            for (Deployment dep : deploymentList.getItems()) {
                String image = "unknown";
                if (dep.getSpec() != null && dep.getSpec().getTemplate() != null &&
                        dep.getSpec().getTemplate().getSpec() != null &&
                        !dep.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {
                    image = dep.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
                }

                K8sDeploymentDto dto = new K8sDeploymentDto(
                        dep.getMetadata().getName(),
                        dep.getMetadata().getNamespace(),
                        dep.getSpec() != null ? dep.getSpec().getReplicas() : 1,
                        dep.getStatus() != null ? dep.getStatus().getReadyReplicas() : 0,
                        dep.getStatus() != null ? dep.getStatus().getAvailableReplicas() : 0,
                        image,
                        dep.getMetadata().getCreationTimestamp()
                );
                dto.setLabels(dep.getMetadata().getLabels());
                if (dep.getStatus() != null) {
                    dto.setUpdatedReplicas(dep.getStatus().getUpdatedReplicas());
                }
                result.add(dto);
            }
            return result;
        } catch (Exception ex) {
            handleK8sException("getDeployments", ex);
            return List.of();
        }
    }

    public List<K8sServiceDto> getServices(String configEncrypted, String namespace) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            ServiceList svcList = (namespace != null && !namespace.isBlank() && !"all".equalsIgnoreCase(namespace)) ?
                    client.services().inNamespace(namespace).list() :
                    client.services().inAnyNamespace().list();

            List<K8sServiceDto> result = new ArrayList<>();
            for (io.fabric8.kubernetes.api.model.Service svc : svcList.getItems()) {
                List<String> ports = new ArrayList<>();
                if (svc.getSpec() != null && svc.getSpec().getPorts() != null) {
                    for (ServicePort sp : svc.getSpec().getPorts()) {
                        ports.add(sp.getPort() + (sp.getNodePort() != null ? ":" + sp.getNodePort() : "") + "/" + sp.getProtocol());
                    }
                }

                K8sServiceDto dto = new K8sServiceDto(
                        svc.getMetadata().getName(),
                        svc.getMetadata().getNamespace(),
                        svc.getSpec() != null ? svc.getSpec().getType() : "ClusterIP",
                        svc.getSpec() != null ? svc.getSpec().getClusterIP() : "None",
                        ports,
                        svc.getMetadata().getCreationTimestamp()
                );
                if (svc.getSpec() != null) {
                    dto.setSelector(svc.getSpec().getSelector());
                }
                result.add(dto);
            }
            return result;
        } catch (Exception ex) {
            handleK8sException("getServices", ex);
            return List.of();
        }
    }

    public List<String> getPodLogs(String configEncrypted, String namespace, String podName, String containerName, int tailLines) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            var logResource = (containerName != null && !containerName.isBlank()) ?
                    client.pods().inNamespace(namespace).withName(podName).inContainer(containerName) :
                    client.pods().inNamespace(namespace).withName(podName);

            String logOutput = logResource.tailingLines(Math.max(1, tailLines)).getLog();
            if (logOutput == null || logOutput.isBlank()) {
                return List.of();
            }
            return Arrays.asList(logOutput.split("\r?\n"));
        } catch (Exception ex) {
            handleK8sException("getPodLogs", ex);
            return List.of();
        }
    }

    public Map<String, Object> restartDeployment(String configEncrypted, String namespace, String deploymentName) {
        try (KubernetesClient client = createClient(configEncrypted)) {
            Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
            if (deployment == null) {
                throw new ResourceNotFoundException("KubernetesDeployment", deploymentName);
            }

            // Perform standard kubectl rollout restart by updating template annotation
            client.apps().deployments().inNamespace(namespace).withName(deploymentName)
                    .rolling()
                    .restart();

            log.info("Triggered rollout restart on deployment {}/{}", namespace, deploymentName);
            return Map.of(
                    "status", "SUCCESS",
                    "deployment", deploymentName,
                    "namespace", namespace,
                    "restartedAt", Instant.now().toString()
            );
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            handleK8sException("restartDeployment", ex);
            return Map.of();
        }
    }

    public Map<String, Object> testConnection(String configEncrypted) {
        long start = System.currentTimeMillis();
        try (KubernetesClient client = createClient(configEncrypted)) {
            var version = client.getKubernetesVersion();
            long latency = System.currentTimeMillis() - start;

            return Map.of(
                    "status", "HEALTHY",
                    "connected", true,
                    "latencyMs", latency,
                    "gitVersion", version != null ? version.getGitVersion() : "unknown",
                    "major", version != null ? version.getMajor() : "1",
                    "minor", version != null ? version.getMinor() : "29",
                    "checkedAt", Instant.now().toString()
            );
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorCode", "K8S_CONNECTION_FAILED",
                    "errorMessage", ex.getMessage() != null ? ex.getMessage() : "Failed to connect to Kubernetes cluster"
            );
        }
    }

    private KubernetesClient createClient(String configEncrypted) {
        if (configEncrypted != null && !configEncrypted.isBlank()) {
            try {
                String decryptedKubeconfig = cryptoService.decrypt(configEncrypted);
                if (decryptedKubeconfig != null && !decryptedKubeconfig.isBlank()) {
                    Config config = Config.fromKubeconfig(decryptedKubeconfig);
                    return new KubernetesClientBuilder().withConfig(config).build();
                }
            } catch (Exception ex) {
                log.warn("Failed to create Kubernetes client from encrypted kubeconfig, falling back to auto-config: {}", ex.getMessage());
            }
        }
        return new KubernetesClientBuilder().build();
    }

    private void handleK8sException(String operation, Exception ex) {
        log.error("Kubernetes operation [{}] failed: {}", operation, ex.getMessage());
        if (ex instanceof KubernetesClientException kce) {
            if (kce.getCode() == 401 || kce.getCode() == 403) {
                throw new IntegrationForbiddenException("KUBERNETES", "RBAC access denied for operation: " + operation);
            }
            if (kce.getCode() == 404) {
                throw new ResourceNotFoundException("KubernetesResource", operation);
            }
            throw new IntegrationUnavailableException("KUBERNETES", "Cluster communication error: " + kce.getMessage(), kce);
        }
        if (ex instanceof IntegrationException ie) {
            throw ie;
        }
        throw new IntegrationUnavailableException("KUBERNETES", "Kubernetes cluster is unavailable: " + ex.getMessage(), ex);
    }
}
