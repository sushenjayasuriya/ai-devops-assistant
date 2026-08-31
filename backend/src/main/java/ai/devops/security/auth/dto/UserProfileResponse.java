package ai.devops.security.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private UUID organizationId;
    private String organizationName;
    private List<String> roles;
    private Instant lastLoginAt;

    public UserProfileResponse() {}

    public UserProfileResponse(UUID id, String email, String fullName, UUID organizationId, String organizationName, List<String> roles, Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.roles = roles;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
