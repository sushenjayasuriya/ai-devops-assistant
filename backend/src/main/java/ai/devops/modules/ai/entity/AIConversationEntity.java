package ai.devops.modules.ai.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.incident.entity.IncidentEntity;
import ai.devops.modules.user.entity.UserEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversations")
public class AIConversationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    private EnvironmentEntity environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentEntity incident;

    @Column(name = "title", nullable = false)
    private String title;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AIMessageEntity> messages = new ArrayList<>();

    public AIConversationEntity() {}

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public EnvironmentEntity getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment;
    }

    public IncidentEntity getIncident() {
        return incident;
    }

    public void setIncident(IncidentEntity incident) {
        this.incident = incident;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<AIMessageEntity> getMessages() {
        return messages;
    }

    public void setMessages(List<AIMessageEntity> messages) {
        this.messages = messages;
    }
}
