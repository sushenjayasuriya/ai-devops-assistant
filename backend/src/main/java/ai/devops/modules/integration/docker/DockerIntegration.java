package ai.devops.modules.integration.docker;

import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import ai.devops.modules.integration.docker.client.DockerEngineClient;
import ai.devops.modules.integration.docker.dto.DockerContainerDetails;
import ai.devops.modules.integration.docker.dto.DockerContainerStats;
import ai.devops.modules.integration.docker.dto.DockerContainerSummary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DockerIntegration implements InfrastructureIntegration {

    private final DockerEngineClient dockerClient;

    public DockerIntegration(DockerEngineClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.DOCKER;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        Map<String, Object> result = dockerClient.testConnection(endpointUrl);
        return Boolean.TRUE.equals(result.get("connected"));
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return dockerClient.testConnection(endpointUrl);
    }

    public List<DockerContainerSummary> listContainers(String endpointUrl, boolean all) {
        return dockerClient.listContainers(endpointUrl, all);
    }

    public DockerContainerDetails inspectContainer(String endpointUrl, String containerId) {
        return dockerClient.inspectContainer(endpointUrl, containerId);
    }

    public DockerContainerStats getContainerStats(String endpointUrl, String containerId) {
        return dockerClient.getContainerStats(endpointUrl, containerId);
    }

    public List<String> getContainerLogs(String endpointUrl, String containerId, int tail) {
        return dockerClient.getContainerLogs(endpointUrl, containerId, tail);
    }

    public void restartContainer(String endpointUrl, String containerId) {
        dockerClient.restartContainer(endpointUrl, containerId);
    }

    public void stopContainer(String endpointUrl, String containerId) {
        dockerClient.stopContainer(endpointUrl, containerId);
    }

    public void startContainer(String endpointUrl, String containerId) {
        dockerClient.startContainer(endpointUrl, containerId);
    }
}
