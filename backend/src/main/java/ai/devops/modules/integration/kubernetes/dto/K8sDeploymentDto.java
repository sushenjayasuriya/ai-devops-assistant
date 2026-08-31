package ai.devops.modules.integration.kubernetes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sDeploymentDto {
    private String name;
    private String namespace;
    private Integer replicas;
    private Integer readyReplicas;
    private Integer availableReplicas;
    private Integer updatedReplicas;
    private String image;
    private String createdAt;
    private Map<String, String> labels;

    public K8sDeploymentDto() {}

    public K8sDeploymentDto(String name, String namespace, Integer replicas, Integer readyReplicas, Integer availableReplicas, String image, String createdAt) {
        this.name = name;
        this.namespace = namespace;
        this.replicas = replicas;
        this.readyReplicas = readyReplicas;
        this.availableReplicas = availableReplicas;
        this.image = image;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Integer getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(Integer readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public Integer getAvailableReplicas() {
        return availableReplicas;
    }

    public void setAvailableReplicas(Integer availableReplicas) {
        this.availableReplicas = availableReplicas;
    }

    public Integer getUpdatedReplicas() {
        return updatedReplicas;
    }

    public void setUpdatedReplicas(Integer updatedReplicas) {
        this.updatedReplicas = updatedReplicas;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
}
