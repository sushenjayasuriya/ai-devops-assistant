package ai.devops.modules.integration.docker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerContainerStats {
    private String containerId;
    private String name;
    private Double cpuPercent;
    private Long memoryUsageBytes;
    private Long memoryLimitBytes;
    private Double memoryPercent;
    private Long networkRxBytes;
    private Long networkTxBytes;
    private Integer pids;

    public DockerContainerStats() {}

    public DockerContainerStats(String containerId, String name, Double cpuPercent, Long memoryUsageBytes, Long memoryLimitBytes, Double memoryPercent, Long networkRxBytes, Long networkTxBytes, Integer pids) {
        this.containerId = containerId;
        this.name = name;
        this.cpuPercent = cpuPercent;
        this.memoryUsageBytes = memoryUsageBytes;
        this.memoryLimitBytes = memoryLimitBytes;
        this.memoryPercent = memoryPercent;
        this.networkRxBytes = networkRxBytes;
        this.networkTxBytes = networkTxBytes;
        this.pids = pids;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(Double cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    public void setMemoryUsageBytes(Long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }

    public Long getMemoryLimitBytes() {
        return memoryLimitBytes;
    }

    public void setMemoryLimitBytes(Long memoryLimitBytes) {
        this.memoryLimitBytes = memoryLimitBytes;
    }

    public Double getMemoryPercent() {
        return memoryPercent;
    }

    public void setMemoryPercent(Double memoryPercent) {
        this.memoryPercent = memoryPercent;
    }

    public Long getNetworkRxBytes() {
        return networkRxBytes;
    }

    public void setNetworkRxBytes(Long networkRxBytes) {
        this.networkRxBytes = networkRxBytes;
    }

    public Long getNetworkTxBytes() {
        return networkTxBytes;
    }

    public void setNetworkTxBytes(Long networkTxBytes) {
        this.networkTxBytes = networkTxBytes;
    }

    public Integer getPids() {
        return pids;
    }

    public void setPids(Integer pids) {
        this.pids = pids;
    }
}
