package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Entity
@Table(name = "categories")
// Explicitly exclude lazy-loaded and circular relations from toString
@ToString(callSuper = true, exclude = {"parent", "subCategories"})
public class Category extends AbstractEntity {
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    public String name;

    @NotNull
    @Column(name = "description", nullable = false)
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    public Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    public Set<Category> subCategories = new HashSet<>();

    public Category(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Category category = (Category) obj;

        // If the entity has an ID (persisted), prefer ID equality.
        // Otherwise, fall back to business keys using getters to safely handle proxies.
        if (this.id != null && category.id != null) {
            return Objects.equals(this.id, category.id);
        }
        return Objects.equals(this.name, category.name) &&
                Objects.equals(this.description, category.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.description);
    }
}
