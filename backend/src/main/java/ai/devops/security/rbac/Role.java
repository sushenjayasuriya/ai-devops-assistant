package ai.devops.security.rbac;

public enum Role {
    ADMIN,
    DEVOPS_ENGINEER,
    VIEWER;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
