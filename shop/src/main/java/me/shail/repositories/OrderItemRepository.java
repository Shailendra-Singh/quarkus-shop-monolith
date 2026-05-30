package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.shail.models.OrderItem;
import me.shail.repositories.managed.OrderItemCommandRepository;
import me.shail.repositories.stateless.OrderItemQueryRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderItemRepository {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderItemCommandRepository orderItemCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderItemQueryRepository orderItemQueryRepository;

    public Uni<OrderItem> create(OrderItem orderItem) {
        return this.orderItemCommandRepository.persist(orderItem).replaceWith(orderItem);
    }

    public Uni<Boolean> deleteById(UUID orderItemId) {
        return this.orderItemCommandRepository.deleteById(orderItemId);
    }

    public Uni<List<OrderItem>> findAllByOrderId(UUID orderId) {
        return this.orderItemQueryRepository.findAllByOrderId(orderId);
    }

    public Uni<OrderItem> findByIdManaged(UUID orderItemId) {
        return this.orderItemCommandRepository.findByIdWithOrderAndProduct(orderItemId)
                .onFailure().recoverWithItem((OrderItem) null);
    }

    public Uni<OrderItem> findByIdStateless(UUID orderItemId) {
        return this.orderItemQueryRepository.findByIdWithOrderAndProduct(orderItemId)
                .onFailure().recoverWithItem((OrderItem) null);
    }
}
