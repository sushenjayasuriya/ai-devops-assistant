package ai.devops.modules.infrastructure.container.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.infrastructure.server.entity.ServerEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "containers")
public class ContainerEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private ServerEntity server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private EnvironmentEntity environment;

    @Column(name = "container_id", nullable = false, length = 100)
    private String containerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "state", nullable = false)
    private String state; // RUNNING, EXITED, RESTARTING, DEAD

    @Column(name = "restart_count", nullable = false)
    private int restartCount = 0;

    @Column(name = "port_mappings", columnDefinition = "TEXT")
    private String portMappings;

    @Column(name = "started_at")
    private Instant startedAt;

    public ContainerEntity() {}

    public ServerEntity getServer() {
        return server;
    }

    public void setServer(ServerEntity server) {
        this.server = server;
    }

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }

    public String getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(String portMappings) {
        this.portMappings = portMappings;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
}
