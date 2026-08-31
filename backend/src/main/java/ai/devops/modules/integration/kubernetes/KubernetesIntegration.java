package ai.devops.modules.integration.kubernetes;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.kubernetes.client.KubernetesClientService;
import ai.devops.modules.integration.kubernetes.dto.K8sDeploymentDto;
import ai.devops.modules.integration.kubernetes.dto.K8sNamespaceDto;
import ai.devops.modules.integration.kubernetes.dto.K8sPodDto;
import ai.devops.modules.integration.kubernetes.dto.K8sServiceDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KubernetesIntegration implements InfrastructureIntegration {

    private final KubernetesClientService k8sClientService;

    public KubernetesIntegration(KubernetesClientService k8sClientService) {
        this.k8sClientService = k8sClientService;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.KUBERNETES;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        Map<String, Object> result = k8sClientService.testConnection(configEncrypted);
        return Boolean.TRUE.equals(result.get("connected"));
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return k8sClientService.testConnection(configEncrypted);
    }

    public List<K8sNamespaceDto> getNamespaces(String configEncrypted) {
        return k8sClientService.getNamespaces(configEncrypted);
    }

    public List<K8sPodDto> getPods(String configEncrypted, String namespace) {
        return k8sClientService.getPods(configEncrypted, namespace);
    }

    public List<K8sDeploymentDto> getDeployments(String configEncrypted, String namespace) {
        return k8sClientService.getDeployments(configEncrypted, namespace);
    }

    public List<K8sServiceDto> getServices(String configEncrypted, String namespace) {
        return k8sClientService.getServices(configEncrypted, namespace);
    }

    public List<String> getPodLogs(String configEncrypted, String namespace, String podName, String containerName, int tailLines) {
        return k8sClientService.getPodLogs(configEncrypted, namespace, podName, containerName, tailLines);
    }

    public Map<String, Object> restartDeployment(String configEncrypted, String namespace, String deploymentName) {
        return k8sClientService.restartDeployment(configEncrypted, namespace, deploymentName);
    }
}
