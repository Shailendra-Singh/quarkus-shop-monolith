package me.shail.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.OrderDto;
import me.shail.dtos.OrderItemDto;
import me.shail.models.Address;
import me.shail.models.Order;
import me.shail.models.enums.OrderStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.PaymentRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@ApplicationScoped
public class OrderService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderRepository orderRepository;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    PaymentRepository paymentRepository;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CartRepository cartRepository;

    public static OrderDto mapToDto(Order order) {
        Set<OrderItemDto> orderItems = order.orderItems
                .stream()
                .map(OrderItemService::mapToDto)
                .collect(Collectors.toSet());

        return new OrderDto(
                order.id,
                order.price,
                order.status.name(),
                order.shipped,
                order.payment != null ? order.payment.id : null,
                AddressService.mapToDto(order.shipmentAddress),
                orderItems,
                CartService.mapToDto(order.cart)
        );
    }

    public Uni<List<OrderDto>> findAll() {
        log.debug("Request to get all Orders");
        return this.orderRepository.findAll().list().onItem().transformToUni(orders -> {
            return Uni.createFrom()
                    .item(orders.stream()
                            .map(OrderService::mapToDto)
                            .toList()
                    );
        });
    }

    public Uni<OrderDto> findById(UUID id) {
        log.debug("Request to get Order: {}", id);
        return this.orderRepository.findById(id).map(OrderService::mapToDto);
    }

    public Uni<List<OrderDto>> findAllByUser(UUID id) {
        return this.orderRepository.findByCart_Customer_Id(id)
                .onItem().transformToUni(orders -> {
                    return Uni.createFrom().item(orders.stream().map(OrderService::mapToDto).toList());
                });

    }

    public Uni<OrderDto> create(OrderDto orderDto) {
        log.debug("Request to create Order: {}", orderDto);
        UUID cartId = orderDto.cart().id();
        return this.cartRepository.findById(cartId)
                .onItem().ifNull().failWith(new IllegalStateException("Cart not found with Id: [" + cartId + "]"))
                .chain(cart -> {
                    Address shipmentAddress = AddressService.createFromDto(orderDto.shipmentAddress());
                    Order order = new Order(
                            BigDecimal.ZERO,
                            OrderStatus.CREATION,
                            null,
                            null,
                            shipmentAddress,
                            Collections.emptySet(),
                            cart
                    );
                    return this.orderRepository.insert(order).replaceWith(order);
                }).onItem().transform(OrderService::mapToDto);
    }

    public Uni<Boolean> delet(UUID id) {
        log.debug("Request to delete Order: {}", id);
        return this.orderRepository.findById(id)
                .onItem()
                .ifNull()
                .failWith(() -> new IllegalStateException("Order with ID [" + id + "] cannot be found!"))
                .chain(order -> {
                    Uni<Boolean> deleteOrder = this.orderRepository.deleteById(id);
                    if (order.payment != null) {
                        return Uni.combine().all().unis(
                                deleteOrder,
                                this.paymentRepository.deleteById(order.payment.id)
                        ).asTuple().onItem().transform(Tuple2::getItem1);
                    }
                    return deleteOrder;
                });
    }

    public Uni<Boolean> existsById(UUID id) {
        return this.orderRepository.existsById(id);
    }
}
