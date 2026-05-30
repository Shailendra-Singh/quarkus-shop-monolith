package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@NoArgsConstructor
@ToString(callSuper = true, exclude = "product")
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_product_rating", columnList = "product_id, rating DESC")
        }
)
public class Review extends AbstractEntity {
    @NotNull
    @Column(name = "title", nullable = false)
    public String title;

    @NotNull
    @Column(name = "description", nullable = false)
    public String description;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    public Integer rating;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Product product;

    public Review(@NotNull String title, @NotNull String description, @NotNull Integer rating, @NotNull Product product) {
        this.title = title;
        this.description = description;
        this.rating = rating;
        this.product = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review review)) return false;
        return id != null && id.equals(review.id);
    }

    @Override
    public int hashCode() {
        // Use a constant or just the ID class
        return getClass().hashCode();
    }
}