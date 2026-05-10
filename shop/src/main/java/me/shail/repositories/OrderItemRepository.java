package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.OrderItem;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends PanacheRepository.Reactive.Stateless<OrderItem, UUID> {
    @HQL("from OrderItem where order.id = :orderId")
    Uni<List<OrderItem>> findAllByOrderId(UUID orderId);
}
