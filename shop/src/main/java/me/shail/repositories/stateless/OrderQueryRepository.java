package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Order;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface OrderQueryRepository extends PanacheRepository.Reactive.Stateless<Order, UUID> {
    /**
     * Finds the order by customer id.
     * Navigates: Order -> Cart -> Customer -> id
     *
     * @param customerId : customer id
     * @return Order
     */
    @HQL("from Order o" +
            " left join fetch o.orderItems" +
            " left join fetch o.cart c1" +
            " left join fetch c1.customer c2" +
            " where c2.id = :customerId")
    Uni<List<Order>> findOrderByCustomerId(UUID customerId);

    /**
     * Find an order by payment id. Order:Payment is 1:1
     *
     * @param paymentId : payment id
     * @return Order
     */
    @HQL("from Order o where o.payment.id = :paymentId")
    Uni<Order> findOrderByPaymentId(UUID paymentId);

    /**
     * Checks if an order exists
     *
     * @param orderId : order id
     * @return true/false
     */
    default Uni<Boolean> existsById(UUID orderId) {
        return count("id = ?1", orderId)
                .map(count -> count > 0);
    }

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
                " where o.id = ?1", orderId).firstResult();
    }

    /**
     * Finds all orders. Eagerly fetches its associated order items as well
     * and cart and cart-customer
     *
     * @return order
     */
    default Uni<List<Order>> listAllWithSubItems() {
        return find("from Order o " +
                " left join fetch o.orderItems" +
                " left join fetch o.cart c" +
                " left join fetch c.customer").list();
    }
}
