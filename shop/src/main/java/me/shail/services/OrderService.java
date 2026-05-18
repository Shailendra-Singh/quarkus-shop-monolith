package me.shail.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.OrderDto;
import me.shail.dtos.OrderItemDto;
import me.shail.models.Address;
import me.shail.models.Order;
import me.shail.models.enums.OrderStatus;
import me.shail.repositories.CustomerRepository;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.PaymentRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@ApplicationScoped
public class OrderService {
    @Inject
    OrderRepository orderRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    PaymentRepository paymentRepository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    CartService cartService;

    public static OrderDto mapToDto(Order order) {
        Set<OrderItemDto> orderItems = order.orderItems == null || order.orderItems.isEmpty()
                ? Collections.emptySet() : order.orderItems
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

    public Uni<List<OrderDto>> listAll() {
        log.debug("Request to get all Orders");
        return this.orderRepository.listAllWithSubItems().onItem()
                .transformToUni(orders -> Uni.createFrom()
                        .item(orders.stream()
                                .map(OrderService::mapToDto)
                                .toList()
                        ));
    }

    public Uni<OrderDto> findById(UUID orderId) {
        log.debug("Request to get Order: {}", orderId);
        return generateUni_FindById(orderId, false)
                .map(OrderService::mapToDto);
    }

    public Uni<List<OrderDto>> findAllByCustomer(UUID customerId) {
        return CustomerService.generateUni_FindCustomerById(customerRepository, customerId, false)
                .chain(_ ->
                        this.orderRepository.findOrderByCustomerId(customerId)
                                .onItem().transformToUni(orders -> Uni.createFrom()
                                        .item(orders.stream()
                                                .map(OrderService::mapToDto)
                                                .toList()
                                        )
                                )
                );

    }

    public Uni<OrderDto> create(OrderDto orderDto) {
        log.debug("Request to create Order: {}", orderDto);
        UUID cartId = orderDto.cart().id();
        return cartService.generateUni_FindCartWithCustomer(cartId, true)
                .chain(cart -> {
                    Address shipmentAddress = AddressService.createFromDto(orderDto.shipmentAddress());
                    Order order = new Order(
                            orderDto.price(),
                            OrderStatus.CREATION,
                            orderDto.shipped(),
                            null,
                            shipmentAddress,
                            Collections.emptySet(),
                            cart
                    );
                    return this.orderRepository.create(order).replaceWith(order);
                }).onItem().transform(OrderService::mapToDto);
    }

    public Uni<Boolean> delete(UUID orderId) {
        log.debug("Request to delete Order: {}", orderId);
        return generateUni_FindById(orderId, true)
                .chain(order -> {
                    Uni<Boolean> deleteOrder = this.orderRepository.delete(orderId);
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

    private Uni<Order> generateUni_FindById(UUID orderId, boolean managed) {
        Uni<Order> generatedUni;
        if (managed)
            generatedUni = this.orderRepository.findOrderByIdWithOrderItemsManaged(orderId);
        else
            generatedUni = this.orderRepository.findOrderByIdWithOrderItemsStateless(orderId);

        return generatedUni
                .onItem().ifNull().failWith(() ->
                        new EntityNotFoundException("The Order does not exist! Id: " + orderId)
                );
    }
}
