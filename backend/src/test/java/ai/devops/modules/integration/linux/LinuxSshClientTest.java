package ai.devops.modules.integration.linux;

import ai.devops.modules.integration.linux.client.LinuxSshClient;
import ai.devops.modules.integration.linux.model.LinuxServerTelemetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinuxSshClientTest {

    @Test
    @DisplayName("Linux uptime output is parsed correctly into load averages")
    void testParseUptime() {
        String raw = " 14:20:15 up 12 days,  4:12,  2 users,  load average: 1.45, 0.82, 0.55";
        LinuxServerTelemetry telemetry = new LinuxServerTelemetry();
        LinuxSshClient.parseUptime(raw, telemetry);

        assertEquals(1.45, telemetry.getLoadAverage1m());
        assertEquals(0.82, telemetry.getLoadAverage5m());
        assertEquals(0.55, telemetry.getLoadAverage15m());
        assertEquals(raw, telemetry.getUptimeString());
    }

    @Test
    @DisplayName("Linux free -m memory output is parsed correctly")
    void testParseMemory() {
        String raw = """
                       total        used        free      shared  buff/cache   available
        Mem:           16384        8192        4096         256        4096        7840
        Swap:           4096         512        3584
        """;
        LinuxServerTelemetry telemetry = new LinuxServerTelemetry();
        LinuxSshClient.parseMemory(raw, telemetry);

        assertNotNull(telemetry.getMemory());
        assertEquals(16384L, telemetry.getMemory().getTotalMb());
        assertEquals(8192L, telemetry.getMemory().getUsedMb());
        assertEquals(4096L, telemetry.getMemory().getFreeMb());
        assertEquals(7840L, telemetry.getMemory().getAvailableMb());
        assertEquals(50.0, telemetry.getMemory().getUsedPercent());
    }

    @Test
    @DisplayName("Linux df -h disk output is parsed correctly")
    void testParseDisk() {
        String raw = """
        Filesystem      Size  Used Avail Use% Mounted on
        /dev/sda1        50G   25G   25G  50% /
        /dev/sdb1       100G   80G   20G  80% /data
        """;
        LinuxServerTelemetry telemetry = new LinuxServerTelemetry();
        LinuxSshClient.parseDisk(raw, telemetry);

        assertNotNull(telemetry.getDisks());
        assertEquals(2, telemetry.getDisks().size());
        assertEquals("/dev/sda1", telemetry.getDisks().get(0).getFilesystem());
        assertEquals("50G", telemetry.getDisks().get(0).getSize());
        assertEquals("/data", telemetry.getDisks().get(1).getMountedOn());
    }

    @Test
    @DisplayName("Linux ps aux process output is parsed correctly")
    void testParseProcesses() {
        String raw = """
        USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
        postgres  1204  4.5  6.2 345000 98000 ?        S    10:00   0:15 postgres: checkpointer
        devops    4501 12.8 14.5 980000 240000 ?       S    10:05   1:30 java -jar app.jar
        """;
        LinuxServerTelemetry telemetry = new LinuxServerTelemetry();
        LinuxSshClient.parseProcesses(raw, telemetry);

        assertNotNull(telemetry.getTopProcesses());
        assertEquals(2, telemetry.getTopProcesses().size());
        assertEquals("postgres", telemetry.getTopProcesses().get(0).getUser());
        assertEquals(1204, telemetry.getTopProcesses().get(0).getPid());
        assertEquals(4.5, telemetry.getTopProcesses().get(0).getCpuPercent());
        assertEquals("java -jar app.jar", telemetry.getTopProcesses().get(1).getCommand());
    }
}
