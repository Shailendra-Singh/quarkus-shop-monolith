package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.CartStatus;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(
        name = "carts",
        indexes = {
                @Index(name = "idx_carts_customer_status", columnList = "customer_id, status")
        }
)
public class Cart extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY) // Industry standard: Always use Lazy for ManyToOne
    public Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CartStatus status;

    public Cart(Customer customer, @NotNull CartStatus status) {
        this.customer = customer;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart cart)) return false;
        // Relying strictly on the AbstractEntity ID to prevent Lazy load crashes
        return id != null && id.equals(cart.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Standard safe practice for Hibernate entities
    }
}