package ai.devops.modules.integration.linux.client;

import ai.devops.common.exception.IntegrationAuthException;
import ai.devops.common.exception.IntegrationInvalidResponseException;
import ai.devops.common.exception.IntegrationTimeoutException;
import ai.devops.common.exception.IntegrationUnavailableException;
import ai.devops.modules.integration.linux.model.LinuxCommand;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import ai.devops.security.ssrf.SsrfProtectionValidator;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LinuxSshClient {

    private static final Logger log = LoggerFactory.getLogger(LinuxSshClient.class);
    private static final int MAX_BUFFER_BYTES = 65536; // 64 KB safety buffer

    private final SsrfProtectionValidator ssrfValidator;

    public LinuxSshClient(SsrfProtectionValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
    }

    public String executeTypedCommand(String host, int port, String user, String password, String privateKey, LinuxCommand command, int timeoutMs) {
        ssrfValidator.validateEndpointUrl("http://" + host + ":" + port);

        log.info("Executing safe typed SSH command [{}] on host {}:{} user [{}]", command.name(), host, port, user);

        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 5000);
            ssh.setTimeout(timeoutMs > 0 ? timeoutMs : 5000);

            ssh.connect(host, port > 0 ? port : 22);

            if (privateKey != null && !privateKey.isBlank()) {
                ssh.authPublickey(user, ssh.loadKeys(privateKey, null, null));
            } else if (password != null && !password.isBlank()) {
                ssh.authPassword(user, password);
            } else {
                ssh.authPassword(user, "");
            }

            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(command.getShellCommand());
                cmd.join(timeoutMs > 0 ? timeoutMs : 10000, TimeUnit.MILLISECONDS);

                try (InputStream is = cmd.getInputStream()) {
                    byte[] buffer = new byte[MAX_BUFFER_BYTES];
                    int read = is.read(buffer);
                    if (read > 0) {
                        return new String(buffer, 0, read, StandardCharsets.UTF_8).trim();
                    }
                    return "";
                }
            }
        } catch (UserAuthException ex) {
            throw new IntegrationAuthException("LINUX_SSH", "SSH Authentication failed for user '" + user + "' on " + host);
        } catch (java.net.SocketTimeoutException | net.schmizz.sshj.connection.ConnectionException ex) {
            throw new IntegrationTimeoutException("LINUX_SSH", command.name(), timeoutMs > 0 ? timeoutMs : 5000);
        } catch (Exception ex) {
            throw new IntegrationUnavailableException("LINUX_SSH", "Failed to connect to SSH server at " + host + ":" + port + ": " + ex.getMessage(), ex);
        }
    }

    public LinuxServerTelemetry collectServerTelemetry(String host, int port, String user, String password, String privateKey, int timeoutMs) {
        LinuxServerTelemetry telemetry = new LinuxServerTelemetry();
        telemetry.setHostname(host);
        telemetry.setIpAddress(host);

        try {
            String uptimeRaw = executeTypedCommand(host, port, user, password, privateKey, LinuxCommand.GET_UPTIME, timeoutMs);
            parseUptime(uptimeRaw, telemetry);

            String memRaw = executeTypedCommand(host, port, user, password, privateKey, LinuxCommand.GET_MEMORY, timeoutMs);
            parseMemory(memRaw, telemetry);

            String diskRaw = executeTypedCommand(host, port, user, password, privateKey, LinuxCommand.GET_DISK, timeoutMs);
            parseDisk(diskRaw, telemetry);

            String procRaw = executeTypedCommand(host, port, user, password, privateKey, LinuxCommand.GET_PROCESSES, timeoutMs);
            parseProcesses(procRaw, telemetry);

            telemetry.setStatus("ONLINE");
            return telemetry;
        } catch (Exception ex) {
            log.error("Failed to collect SSH telemetry from {}: {}", host, ex.getMessage());
            telemetry.setStatus("UNREACHABLE");
            throw ex;
        }
    }

    public Map<String, Object> testConnection(String host, int port, String user, String password, String privateKey) {
        long start = System.currentTimeMillis();
        try {
            String uptime = executeTypedCommand(host, port, user, password, privateKey, LinuxCommand.GET_UPTIME, 4000);
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "HEALTHY",
                    "connected", true,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "uptime", uptime
            );
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return Map.of(
                    "status", "UNHEALTHY",
                    "connected", false,
                    "latencyMs", latency,
                    "checkedAt", Instant.now().toString(),
                    "errorCode", "SSH_CONNECTION_FAILED",
                    "errorMessage", ex.getMessage() != null ? ex.getMessage() : "Unknown SSH error"
            );
        }
    }

    public static void parseUptime(String raw, LinuxServerTelemetry telemetry) {
        if (raw == null || raw.isBlank()) return;
        telemetry.setUptimeString(raw);

        // Pattern for "load average: 0.12, 0.08, 0.05"
        Pattern p = Pattern.compile("load average:\\s*([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+)");
        Matcher m = p.matcher(raw);
        if (m.find()) {
            telemetry.setLoadAverage1m(Double.parseDouble(m.group(1)));
            telemetry.setLoadAverage5m(Double.parseDouble(m.group(2)));
            telemetry.setLoadAverage15m(Double.parseDouble(m.group(3)));
        }
    }

    public static void parseMemory(String raw, LinuxServerTelemetry telemetry) {
        if (raw == null || raw.isBlank()) return;
        // free -m: Mem: total used free shared buff/cache available
        for (String line : raw.split("\r?\n")) {
            if (line.trim().startsWith("Mem:")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 7) {
                    long total = Long.parseLong(parts[1]);
                    long used = Long.parseLong(parts[2]);
                    long free = Long.parseLong(parts[3]);
                    long avail = Long.parseLong(parts[6]);
                    telemetry.setMemory(new LinuxServerTelemetry.MemoryInfo(total, used, free, avail));
                }
            }
        }
    }

    public static void parseDisk(String raw, LinuxServerTelemetry telemetry) {
        if (raw == null || raw.isBlank()) return;
        List<LinuxServerTelemetry.DiskMountInfo> disks = new ArrayList<>();
        String[] lines = raw.split("\r?\n");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].trim().split("\\s+");
            if (parts.length >= 6) {
                disks.add(new LinuxServerTelemetry.DiskMountInfo(
                        parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]
                ));
            }
        }
        telemetry.setDisks(disks);
    }

    public static void parseProcesses(String raw, LinuxServerTelemetry telemetry) {
        if (raw == null || raw.isBlank()) return;
        List<LinuxServerTelemetry.ProcessInfo> processes = new ArrayList<>();
        String[] lines = raw.split("\r?\n");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].trim().split("\\s+", 11);
            if (parts.length >= 11) {
                try {
                    String user = parts[0];
                    int pid = Integer.parseInt(parts[1]);
                    double cpu = Double.parseDouble(parts[2]);
                    double mem = Double.parseDouble(parts[3]);
                    String cmd = parts[10];
                    processes.add(new LinuxServerTelemetry.ProcessInfo(user, pid, cpu, mem, cmd));
                } catch (NumberFormatException ignored) {}
            }
        }
        telemetry.setTopProcesses(processes);
    }
}
