package me.shail.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;

import java.util.Objects;

@NoArgsConstructor
@Entity
@ToString(callSuper = true)
@Table(name = "categories")
public class Category extends AbstractEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    public String name;

    @NotNull
    @Column(name = "description", nullable = false)
    public String description;

    public Category(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Category category)) return false;
        return Objects.equals(name, category.name) && Objects.equals(description, category.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
