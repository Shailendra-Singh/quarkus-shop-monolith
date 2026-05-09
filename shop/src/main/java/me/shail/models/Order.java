package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

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

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(unique = true)
    public Payment payment;

    @Embedded
    public Address shipmentAddress;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    public Set<OrderItem> orderItems;

    @OneToOne
    public Cart cart;

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return Objects.equals(price, order.price)
                && status == order.status
                && Objects.equals(shipped, order.shipped)
                && Objects.equals(payment, order.payment)
                && Objects.equals(shipmentAddress, order.shipmentAddress)
                && Objects.equals(orderItems, order.orderItems)
                && Objects.equals(cart, order.cart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, status, shipped, payment, shipmentAddress, cart);
    }
}
