package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.OrderItem;
import org.hibernate.annotations.processing.HQL;

import java.util.UUID;

public interface OrderItemCommandRepository extends PanacheRepository.Reactive<OrderItem, UUID> {
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
