package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.shail.models.Order;
import me.shail.repositories.managed.OrderCommandRepository;
import me.shail.repositories.stateless.OrderQueryRepository;

import java.util.List;
import java.util.UUID;

@Dependent
public class OrderRepository {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderCommandRepository orderCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderQueryRepository orderQueryRepository;

    public Uni<Order> create(Order newOrder) {
        return this.orderCommandRepository
                .persist(newOrder)
                .replaceWith(newOrder);
    }

    public Uni<Boolean> delete(UUID orderId) {
        return this.orderCommandRepository.deleteById(orderId);
    }

    public Uni<List<Order>> listAll() {
        return orderQueryRepository.listAll();
    }

    public Uni<Order> findById(UUID orderId) {
        return this.orderQueryRepository.findById(orderId);
    }

    public Uni<List<Order>> listAllWithSubItems() {
        return this.orderQueryRepository.listAllWithSubItems();
    }

    public Uni<Order> findOrderByIdWithOrderItemsManaged(UUID orderId) {
        return this.orderCommandRepository.findOrderByIdWithSubItems(orderId);
    }

    public Uni<Order> findOrderByIdWithOrderItemsStateless(UUID orderId) {
        return this.orderQueryRepository.findOrderByIdWithSubItems(orderId);
    }

    public Uni<List<Order>> findOrderByCustomerId(UUID customerId) {
        return this.orderQueryRepository.findOrderByCustomerId(customerId);
    }

    public Uni<Order> findOrderByPaymentId(UUID paymentId) {
        return this.orderQueryRepository.findOrderByPaymentId(paymentId);
    }

    public Uni<Boolean> existsById(UUID orderId) {
        return this.orderQueryRepository.existsById(orderId);
    }
}
