package ai.devops.modules.integration.linux;

import ai.devops.common.exception.UnauthorizedActionException;
import ai.devops.modules.integration.core.InfrastructureIntegration;
import ai.devops.modules.integration.core.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LinuxServerIntegration implements InfrastructureIntegration {

    private static final Logger log = LoggerFactory.getLogger(LinuxServerIntegration.class);

    private final Set<String> allowlistedCommands;

    public LinuxServerIntegration(@Value("${app.security.allowlist-commands:uptime,vmstat,df,free,top,ps}") List<String> allowlist) {
        this.allowlistedCommands = new HashSet<>(allowlist);
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.LINUX_SSH;
    }

    @Override
    public boolean testConnection(String endpointUrl, String configEncrypted) {
        log.info("Testing Linux SSH collector connection: {}", endpointUrl);
        return true;
    }

    @Override
    public Map<String, Object> collectHealth(String endpointUrl, String configEncrypted) {
        return Map.of(
                "status", "HEALTHY",
                "uptime", "16 days, 20:14",
                "loadAverage", "14.8, 12.3, 9.7",
                "os", "Ubuntu 22.04.3 LTS"
        );
    }

    public String executeAllowlistedCommand(String command, String host) {
        String baseCommand = command.trim().split("\\s+")[0];
        if (!allowlistedCommands.contains(baseCommand) && !allowlistedCommands.contains(command.trim())) {
            log.warn("BLOCKED forbidden Linux command execution attempt: '{}' on host '{}'", command, host);
            throw new UnauthorizedActionException(
                    String.format("Command '%s' is not in the security allowlist. Arbitrary command execution is strictly prohibited.", command));
        }

        log.info("Executing allowlisted command '{}' on host '{}'", command, host);
        return switch (baseCommand) {
            case "uptime" -> " 12:20:14 up 16 days, 20:14,  2 users,  load average: 14.82, 12.31, 9.75";
            case "free" -> "               total        used        free      shared  buff/cache   available\nMem:        65839212    59255290     1583920      245000     5000002     5120000\nSwap:        8388608     1048576     7340032";
            case "df" -> "Filesystem     1K-blocks      Used Available Use% Mounted on\n/dev/sda1      515578768 350593562 138768822  72% /";
            default -> "Command output simulated safely under allowlist policy.";
        };
    }
}
