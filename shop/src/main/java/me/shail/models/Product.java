package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "products")
public class Product extends AbstractEntity {
    @NotNull
    @Column(name = "name", nullable = false)
    public String name;

    @NotNull
    @Column(name = "description", nullable = false)
    public String description;

    @NotNull
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    public BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public ProductStatus status;

    @Column(name = "sales_counter")
    public Integer salesCounter;

    // mappedBy refers to the 'product' field in the Review class
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Review> reviews = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "category_id")
    public Category category;

    public Product(String name
            , @NotNull String description
            , @NotNull BigDecimal price
            , @NotNull ProductStatus status
            , @NotNull Integer salesCounter
            , Set<Review> reviews
            , Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.salesCounter = salesCounter;
        this.reviews = reviews;
        this.category = category;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return Objects.equals(name, product.name)
                && Objects.equals(description, product.description)
                && Objects.equals(price, product.price)
                && status == product.status
                && Objects.equals(salesCounter, product.salesCounter)
                && Objects.equals(reviews, product.reviews)
                && Objects.equals(category, product.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, price, category);
    }
}
