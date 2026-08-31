package ai.devops.modules.infrastructure.server.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.integration.core.entity.IntegrationEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "servers")
public class ServerEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id")
    private IntegrationEntity integration;

    @Column(name = "hostname", nullable = false)
    private String hostname;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "ssh_port", nullable = false)
    private Integer sshPort = 22;

    @Column(name = "ssh_user", nullable = false)
    private String sshUser = "devops";

    @Column(name = "ssh_credential_encrypted", columnDefinition = "TEXT")
    private String sshCredentialEncrypted;

    @Column(name = "os_info")
    private String osInfo;

    @Column(name = "status", nullable = false)
    private String status = "ONLINE"; // ONLINE, UNREACHABLE, DEGRADED

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    public ServerEntity() {}

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public void setIntegration(IntegrationEntity integration) {
        this.integration = integration;
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

    public Integer getSshPort() {
        return sshPort != null ? sshPort : 22;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getSshUser() {
        return sshUser != null ? sshUser : "devops";
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public String getSshCredentialEncrypted() {
        return sshCredentialEncrypted;
    }

    public void setSshCredentialEncrypted(String sshCredentialEncrypted) {
        this.sshCredentialEncrypted = sshCredentialEncrypted;
    }

    public String getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
