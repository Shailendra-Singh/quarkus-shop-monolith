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
@Table(name = "order_items")
public class OrderItem extends AbstractEntity {

    @NotNull
    @Column(name = "quantity", nullable = false)
    public Long quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    public Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    public Order order;

    public OrderItem(@NotNull Long quantity, Product product, Order order) {
        this.quantity = quantity;
        this.product = product;
        this.order = order;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderItem orderItem = (OrderItem) obj;
        return Objects.equals(quantity, orderItem.quantity) && Objects.equals(product, orderItem.product) && Objects.equals(order, orderItem.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, product, order);
    }
}
