package ai.devops.modules.integration.kubernetes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sPodDto {
    private String name;
    private String namespace;
    private String status; // "Running", "CrashLoopBackOff", "Pending", "Failed"
    private String nodeName;
    private String podIp;
    private Integer restartCount;
    private String createdAt;
    private List<String> containers;
    private Map<String, String> labels;

    public K8sPodDto() {}

    public K8sPodDto(String name, String namespace, String status, String nodeName, String podIp, Integer restartCount, String createdAt, List<String> containers) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.nodeName = nodeName;
        this.podIp = podIp;
        this.restartCount = restartCount;
        this.createdAt = createdAt;
        this.containers = containers;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPodIp() {
        return podIp;
    }

    public void setPodIp(String podIp) {
        this.podIp = podIp;
    }

    public Integer getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(Integer restartCount) {
        this.restartCount = restartCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }
}
