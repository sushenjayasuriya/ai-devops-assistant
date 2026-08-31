package ai.devops.modules.environment.entity;

import ai.devops.common.model.BaseEntity;
import ai.devops.modules.user.entity.OrganizationEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "environments")
public class EnvironmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "name", nullable = false)
    private String name; // DEVELOPMENT, STAGING, PRODUCTION

    @Column(name = "description")
    private String description;

    @Column(name = "is_production", nullable = false)
    private boolean isProduction = false;

    public EnvironmentEntity() {}

    public EnvironmentEntity(OrganizationEntity organization, String name, String description, boolean isProduction) {
        this.organization = organization;
        this.name = name;
        this.description = description;
        this.isProduction = isProduction;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isProduction() {
        return isProduction;
    }

    public void setProduction(boolean production) {
        isProduction = production;
    }
}
