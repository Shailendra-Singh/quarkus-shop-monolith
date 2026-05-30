package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.OrderStatus;
import me.shail.models.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "orders")
public class Order extends AbstractEntity {

    @NotNull
    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    public BigDecimal price;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public OrderStatus status;

    @Column(name = "shipped")
    public ZonedDateTime shipped;

    @OneToMany(
            mappedBy = "order",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    public List<Payment> payments = new ArrayList<>();

    @Embedded
    public Address shipmentAddress;

    @OneToMany(mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, // Use ALL since Order owns the lifecycle
            orphanRemoval = true)
    @OrderBy("id ASC") // Ensures items always load in a consistent order
    public Set<OrderItem> orderItems = new LinkedHashSet<>();

    @OneToOne
    public Cart cart;

    public boolean canAcceptNewPayment() {
        if (this.payments == null) return true;

        return this.payments.stream()
                .filter(Objects::nonNull)
                .noneMatch(p -> p.status == PaymentStatus.ACCEPTED || p.status == PaymentStatus.PENDING);
    }

    public Payment getConflictivePayment() {
        return this.payments.stream()
                .filter(p -> p != null && (p.status == PaymentStatus.ACCEPTED || p.status == PaymentStatus.PENDING))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        if (this.id == null || order.id == null) {
            return false;
        }

        return Objects.equals(this.id, order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
