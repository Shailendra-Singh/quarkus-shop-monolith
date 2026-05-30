package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.OrderItem;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface OrderItemQueryRepository extends PanacheRepository.Reactive.Stateless<OrderItem, UUID> {

    /**
     * Finds all order items for a given order
     *
     * @param orderId order id
     * @return list of order items
     */
    @HQL("FROM OrderItem oi " +
            "LEFT JOIN FETCH oi.product p  " +
            "LEFT JOIN FETCH oi.order o " +
            "WHERE o.id = :orderId")
    Uni<List<OrderItem>> findAllByOrderId(UUID orderId);

    /**
     * Finds order item by id
     *
     * @param orderItemId orderItem id
     * @return orderItem
     */
    @HQL("FROM OrderItem oi " +
            "LEFT JOIN FETCH oi.product p  " +
            "LEFT JOIN FETCH oi.order o " +
            "WHERE oi.id = :orderItemId")
    Uni<OrderItem> findByIdWithOrderAndProduct(UUID orderItemId);
}
