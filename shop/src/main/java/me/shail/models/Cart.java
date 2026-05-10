package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.CartStatus;

import java.util.Objects;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "carts")
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cart cart)) return false;
        return Objects.equals(customer.id, cart.customer.id) && Objects.equals(status, cart.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customer.id, status);
    }
}