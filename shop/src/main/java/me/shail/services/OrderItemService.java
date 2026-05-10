package me.shail.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.OrderItemDto;
import me.shail.models.Order;
import me.shail.models.OrderItem;
import me.shail.models.Product;
import me.shail.repositories.OrderItemRepository;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Transactional
@ApplicationScoped
@Slf4j
public class OrderItemService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderItemRepository orderItemRepository;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderRepository orderRepository;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ProductRepository productRepository;

    public static OrderItemDto mapToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.id,
                orderItem.quantity,
                orderItem.product.id,
                orderItem.order.id
        );
    }

    public Uni<OrderItemDto> findById(UUID id) {
        log.debug("Request to get OrderItem: {}", id);
        return this.orderItemRepository.findById(id).onItem().transform(OrderItemService::mapToDto);
    }

    public Uni<OrderItemDto> create(OrderItemDto orderItemDto) {
        log.debug("Request to create OrderItem: {}", orderItemDto);
        return Uni.combine().all()
                // 1. Fetch dependencies in parallel for efficiency
                .unis(
                        this.orderRepository.findById(orderItemDto.orderId())
                                .onItem()
                                .ifNull()
                                .failWith(
                                        new IllegalStateException("The Order does not exist. Id: "
                                                + orderItemDto.orderId())
                                ),
                        this.productRepository.findById(orderItemDto.productId())
                                .onItem()
                                .ifNull()
                                .failWith(
                                        new IllegalStateException("The Product does not exist. Id: "
                                                + orderItemDto.productId())
                                )
                ).asTuple()
                .chain(tuple -> {
                    Order order = tuple.getItem1();
                    Product product = tuple.getItem2();

                    // 2. Use tuple's result to create OrderItem
                    OrderItem orderItem = new OrderItem(orderItemDto.quantity(), product, order);
                    // 3. Update Order State
                    order.price = order.price.add(
                            product.price.multiply(BigDecimal.valueOf(orderItemDto.quantity()))
                    );
                    // 4. Persist
                    return Uni.combine().all().unis(
                                    this.orderItemRepository.insert(orderItem),
                                    this.orderRepository.update(order)
                            ).asTuple()
                            .replaceWith(OrderItemService.mapToDto(orderItem));

                });
    }

    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete OrderItem: {}", id);
        return this.orderItemRepository.findById(id)
                .onItem().ifNull().failWith(new IllegalStateException("OrderItem not found. Id: " + id))
                .chain(orderItem -> {
                    Order order = orderItem.order;
                    // reduce the price due to orderItem's removal
                    BigDecimal reduction = orderItem.product.price.multiply(BigDecimal.valueOf(orderItem.quantity));
                    order.price = order.price.subtract(reduction);

                    return Uni.combine().all().unis(
                                    this.orderItemRepository.deleteById(id),
                                    this.orderRepository.update(order)
                            ).asTuple()
                            .onItem().transform(Tuple2::getItem1);
                });
    }

    public Uni<List<OrderItemDto>> findByOrderId(UUID id) {
        log.debug("Request to get all OrderItems of OrderId {}", id);
        return this.orderItemRepository
                .findAllByOrderId(id)
                .onItem()
                .transformToUni(orderItems -> {
                    return Uni.createFrom().item(orderItems.stream().map(OrderItemService::mapToDto).toList());
                });
    }
}
