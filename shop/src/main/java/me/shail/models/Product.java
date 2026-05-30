package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.ProductStatus;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@Table(name = "products")
// Exclude collections to prevent infinite loops and unintentional lazy loading triggers
@ToString(callSuper = true, exclude = {"reviews", "categories"})
public class Product extends AbstractEntity {

    public static final String TABLE_PRODUCT_CATEGORIES = "product_categories";

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
    public Integer salesCounter = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public Set<Review> reviews = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = Product.TABLE_PRODUCT_CATEGORIES,
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "category_id"})
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Set<Category> categories = new HashSet<>();

    public Product(String name, String description, BigDecimal price, ProductStatus status, Integer salesCounter) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.salesCounter = salesCounter != null ? salesCounter : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Product product = (Product) obj;

        // Prefer Database Identity if available
        if (this.id != null && product.id != null) {
            return Objects.equals(this.id, product.id);
        }

        // Fall back to unique business fields using safe getters (Excluding mutable collections)
        return Objects.equals(this.name, product.name)
                && Objects.equals(this.description, product.description)
                && Objects.equals(this.price, product.price)
                && this.status == product.status;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, description, status);
    }
}