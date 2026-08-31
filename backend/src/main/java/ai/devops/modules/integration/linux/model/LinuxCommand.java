package ai.devops.modules.integration.linux.model;

public enum LinuxCommand {
    GET_UPTIME("uptime", "System uptime and load average"),
    GET_MEMORY("free -m", "RAM and swap memory utilization"),
    GET_DISK("df -h", "Mounted disk space usage"),
    GET_PROCESSES("ps aux --sort=-%cpu | head -n 15", "Top active processes sorted by CPU"),
    GET_LOAD_AVG("cat /proc/loadavg", "Kernel load averages (1m, 5m, 15m)"),
    GET_OS_INFO("cat /etc/os-release", "Operating system release distribution metadata");

    private final String shellCommand;
    private final String description;

    LinuxCommand(String shellCommand, String description) {
        this.shellCommand = shellCommand;
        this.description = description;
    }

    public String getShellCommand() {
        return shellCommand;
    }

    public String getDescription() {
        return description;
    }
}
