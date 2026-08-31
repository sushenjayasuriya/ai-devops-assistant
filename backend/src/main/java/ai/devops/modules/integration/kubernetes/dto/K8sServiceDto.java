package ai.devops.modules.integration.kubernetes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sServiceDto {
    private String name;
    private String namespace;
    private String type; // "ClusterIP", "NodePort", "LoadBalancer"
    private String clusterIp;
    private List<String> ports;
    private Map<String, String> selector;
    private String createdAt;

    public K8sServiceDto() {}

    public K8sServiceDto(String name, String namespace, String type, String clusterIp, List<String> ports, String createdAt) {
        this.name = name;
        this.namespace = namespace;
        this.type = type;
        this.clusterIp = clusterIp;
        this.ports = ports;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClusterIp() {
        return clusterIp;
    }

    public void setClusterIp(String clusterIp) {
        this.clusterIp = clusterIp;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public Map<String, String> getSelector() {
        return selector;
    }

    public void setSelector(Map<String, String> selector) {
        this.selector = selector;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
