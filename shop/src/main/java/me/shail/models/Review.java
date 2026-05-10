package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;

import java.util.Objects;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "reviews")
public class Review extends AbstractEntity {
    @NotNull
    @Column(name = "title", nullable = false)
    public String title;

    @NotNull
    @Column(name = "description", nullable = false)
    public String description;

    @NotNull
    @Column(name = "rating", nullable = false)
    public Long rating;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // This creates the foreign key column
    public Product product;

    public Review(@NotNull String title, @NotNull String description, @NotNull Long rating, @NotNull Product product) {
        this.title = title;
        this.description = description;
        this.rating = rating;
        this.product = product;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Review review = (Review) obj;
        return Objects.equals(title, review.title)
                && Objects.equals(description, review.description)
                && Objects.equals(rating, review.rating)
                && Objects.equals(product, review.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, rating, product);
    }
}