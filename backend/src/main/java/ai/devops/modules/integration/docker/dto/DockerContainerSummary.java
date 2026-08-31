package ai.devops.modules.integration.docker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerContainerSummary {
    private String id;
    private List<String> names;
    private String image;
    private String imageId;
    private String command;
    private Long created;
    private String state; // "running", "exited", "restarting"
    private String status; // "Up 2 hours", "Exited (137) 5 mins ago"

    public DockerContainerSummary() {}

    public DockerContainerSummary(String id, List<String> names, String image, String state, String status) {
        this.id = id;
        this.names = names;
        this.image = image;
        this.state = state;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
