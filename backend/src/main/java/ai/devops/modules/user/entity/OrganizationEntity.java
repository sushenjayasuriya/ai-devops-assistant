package ai.devops.modules.user.entity;

import ai.devops.common.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "organizations")
public class OrganizationEntity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    public OrganizationEntity() {}

    public OrganizationEntity(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
