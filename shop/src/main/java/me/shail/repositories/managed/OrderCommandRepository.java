package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Order;
import me.shail.models.enums.OrderStatus;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface OrderCommandRepository extends PanacheRepository.Reactive<Order, UUID> {
    /**
     * Finds the order by customer id.
     * Navigates: Order -> Cart -> Customer -> id
     *
     * @param customerId : customer id
     * @return Order
     */
    @HQL("from Order o where o.cart.customer.id = :customerId")
    Uni<List<Order>> findOrderByCustomerId(UUID customerId);

    /**
     * Finds an order by id. Eagerly fetches its associated order items as well
     * and cart and cart-customer
     *
     * @param orderId : order id
     * @return order
     */
    default Uni<Order> findOrderByIdWithSubItems(UUID orderId) {
        return find("from Order o " +
                " left join fetch o.orderItems" +
                " left join fetch o.cart c" +
                " left join fetch c.customer" +
                " left join fetch o.payments" +
                " where o.id = ?1", orderId).firstResult();
    }

    /**
     * Find an order by payment id. Order:Payment is 1:1
     *
     * @param paymentId : payment id
     * @return Order
     */
    @HQL("from Order o left join fetch o.payments p where p.id = :paymentId")
    Uni<Order> findOrderByPaymentId(UUID paymentId);

    default Uni<Boolean> cancelOrder(UUID orderId) {
        return update("set status =?1 where id =?2", OrderStatus.CANCELLED, orderId)
                .onItem().transform(rowsUpdated -> rowsUpdated == 1);
    }
}
