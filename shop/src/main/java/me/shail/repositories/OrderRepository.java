package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Order;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends PanacheRepository.Reactive.Stateless<Order, UUID> {
    /**
     * Navigates: Order -> Cart -> Customer -> id
     * Matches the 'id' field of the Customer entity.
     */
    @Find
    Uni<List<Order>> findByCart_Customer_Id(UUID id);

    /**
     * Standard find by Payment ID.
     * Note: Changed from Long to UUID to match your project's identity strategy.
     */
    @Find
    Uni<Order> findByPaymentId(UUID id);

    @HQL("select count(o) > 0 from Order o where o.id = :orderId")
    Uni<Boolean> existsById(UUID orderId);
}
