package ai.devops.modules.integration.linux.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LinuxServerTelemetry {
    private String hostname;
    private String ipAddress;
    private String status;
    private String uptimeString;
    private Long uptimeSeconds;
    private Double loadAverage1m;
    private Double loadAverage5m;
    private Double loadAverage15m;
    private MemoryInfo memory;
    private List<DiskMountInfo> disks;
    private List<ProcessInfo> topProcesses;
    private String osInfo;
    private Instant collectedAt;

    public LinuxServerTelemetry() {
        this.collectedAt = Instant.now();
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUptimeString() {
        return uptimeString;
    }

    public void setUptimeString(String uptimeString) {
        this.uptimeString = uptimeString;
    }

    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Double getLoadAverage1m() {
        return loadAverage1m;
    }

    public void setLoadAverage1m(Double loadAverage1m) {
        this.loadAverage1m = loadAverage1m;
    }

    public Double getLoadAverage5m() {
        return loadAverage5m;
    }

    public void setLoadAverage5m(Double loadAverage5m) {
        this.loadAverage5m = loadAverage5m;
    }

    public Double getLoadAverage15m() {
        return loadAverage15m;
    }

    public void setLoadAverage15m(Double loadAverage15m) {
        this.loadAverage15m = loadAverage15m;
    }

    public MemoryInfo getMemory() {
        return memory;
    }

    public void setMemory(MemoryInfo memory) {
        this.memory = memory;
    }

    public List<DiskMountInfo> getDisks() {
        return disks;
    }

    public void setDisks(List<DiskMountInfo> disks) {
        this.disks = disks;
    }

    public List<ProcessInfo> getTopProcesses() {
        return topProcesses;
    }

    public void setTopProcesses(List<ProcessInfo> topProcesses) {
        this.topProcesses = topProcesses;
    }

    public String getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MemoryInfo {
        private Long totalMb;
        private Long usedMb;
        private Long freeMb;
        private Long availableMb;
        private Double usedPercent;

        public MemoryInfo() {}

        public MemoryInfo(Long totalMb, Long usedMb, Long freeMb, Long availableMb) {
            this.totalMb = totalMb;
            this.usedMb = usedMb;
            this.freeMb = freeMb;
            this.availableMb = availableMb;
            if (totalMb != null && totalMb > 0 && usedMb != null) {
                this.usedPercent = Math.round(((double) usedMb / totalMb) * 1000.0) / 10.0;
            }
        }

        public Long getTotalMb() {
            return totalMb;
        }

        public void setTotalMb(Long totalMb) {
            this.totalMb = totalMb;
        }

        public Long getUsedMb() {
            return usedMb;
        }

        public void setUsedMb(Long usedMb) {
            this.usedMb = usedMb;
        }

        public Long getFreeMb() {
            return freeMb;
        }

        public void setFreeMb(Long freeMb) {
            this.freeMb = freeMb;
        }

        public Long getAvailableMb() {
            return availableMb;
        }

        public void setAvailableMb(Long availableMb) {
            this.availableMb = availableMb;
        }

        public Double getUsedPercent() {
            return usedPercent;
        }

        public void setUsedPercent(Double usedPercent) {
            this.usedPercent = usedPercent;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiskMountInfo {
        private String filesystem;
        private String size;
        private String used;
        private String available;
        private String usePercent;
        private String mountedOn;

        public DiskMountInfo() {}

        public DiskMountInfo(String filesystem, String size, String used, String available, String usePercent, String mountedOn) {
            this.filesystem = filesystem;
            this.size = size;
            this.used = used;
            this.available = available;
            this.usePercent = usePercent;
            this.mountedOn = mountedOn;
        }

        public String getFilesystem() {
            return filesystem;
        }

        public void setFilesystem(String filesystem) {
            this.filesystem = filesystem;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getUsed() {
            return used;
        }

        public void setUsed(String used) {
            this.used = used;
        }

        public String getAvailable() {
            return available;
        }

        public void setAvailable(String available) {
            this.available = available;
        }

        public String getUsePercent() {
            return usePercent;
        }

        public void setUsePercent(String usePercent) {
            this.usePercent = usePercent;
        }

        public String getMountedOn() {
            return mountedOn;
        }

        public void setMountedOn(String mountedOn) {
            this.mountedOn = mountedOn;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessInfo {
        private String user;
        private Integer pid;
        private Double cpuPercent;
        private Double memPercent;
        private String command;

        public ProcessInfo() {}

        public ProcessInfo(String user, Integer pid, Double cpuPercent, Double memPercent, String command) {
            this.user = user;
            this.pid = pid;
            this.cpuPercent = cpuPercent;
            this.memPercent = memPercent;
            this.command = command;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public Integer getPid() {
            return pid;
        }

        public void setPid(Integer pid) {
            this.pid = pid;
        }

        public Double getCpuPercent() {
            return cpuPercent;
        }

        public void setCpuPercent(Double cpuPercent) {
            this.cpuPercent = cpuPercent;
        }

        public Double getMemPercent() {
            return memPercent;
        }

        public void setMemPercent(Double memPercent) {
            this.memPercent = memPercent;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }
}
