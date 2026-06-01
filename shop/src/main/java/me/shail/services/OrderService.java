package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.OrderDto;
import me.shail.dtos.OrderItemDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Address;
import me.shail.models.Order;
import me.shail.models.Payment;
import me.shail.models.enums.OrderStatus;
import me.shail.models.enums.PaymentStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.CustomerRepository;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.PaymentRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class OrderService {

    public final static String ORDER_NOT_EXISTS_ERROR_MSG = "The Order does not exist! Id: ";

    @Inject
    OrderRepository orderRepository;

    @Inject
    PaymentService paymentService;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    CartRepository cartRepository;

    public static OrderDto mapToDto(Order order) {
        Set<OrderItemDto> orderItems = order.orderItems == null || order.orderItems.isEmpty()
                ? Collections.emptySet() : order.orderItems
                .stream()
                .map(OrderItemService::mapToDto)
                .collect(Collectors.toSet());

        UUID recentPaymentId = null;
        if (order.payments != null && !order.payments.isEmpty()) {
            recentPaymentId = order.payments.stream()
                    .sorted(Comparator.comparing((Payment p) -> p.createdAt).reversed())
                    .map(p -> p.id)
                    .findFirst()
                    .orElse(null);

        }

        return new OrderDto(
                order.id,
                order.price,
                order.status.name(),
                order.shipped,
                recentPaymentId,
                AddressService.mapToDto(order.shipmentAddress),
                orderItems,
                CartService.mapToDto(order.cart)
        );
    }

    @WithCustomStatelessSession
    public Uni<List<OrderDto>> listAll() {
        log.debug("Request to get all Orders");
        return this.orderRepository.listAllWithSubItems()
                .map(orders -> orders.stream()
                        .map(OrderService::mapToDto)
                        .toList());
    }

    @WithCustomStatelessSession
    public Uni<List<OrderDto>> findAllByCustomer(UUID customerId) {
        return CustomerService.generateUni_FindCustomerById(customerRepository, customerId, false)
                .chain(_ ->
                        this.orderRepository.findOrderByCustomerId(customerId)
                                .map(orders -> orders.stream()
                                        .map(OrderService::mapToDto)
                                        .toList()
                                ));

    }

    @WithCustomStatelessSession
    public Uni<OrderDto> findOrderByPaymentId(UUID paymentId) {
        return PaymentService.generateUni_FindById(this.paymentRepository, paymentId, false)
                .chain(payment -> this.orderRepository.findOrderByIdWithOrderItemsStateless(payment.order.id))
                .onItem().transform(OrderService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<OrderDto> findById(UUID orderId) {
        log.debug("Request to get Order: {}", orderId);
        return generateUni_FindById(this.orderRepository, orderId, false)
                .map(OrderService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<Boolean> existById(UUID id) {
        return generateUni_ExistById(this.orderRepository, id, false);
    }

    @WithTransaction
    public Uni<OrderDto> create(OrderDto orderDto) {
        log.debug("Request to create Order: {}", orderDto);
        UUID cartId = orderDto.cart().id();
        return CartService.generateUni_FindCartWithCustomer(this.cartRepository, cartId, true)
                .chain(cart -> {
                    Address shipmentAddress = AddressService.createFromDto(orderDto.shipmentAddress());
                    Order order = new Order(
                            orderDto.price(),
                            OrderStatus.CREATION,
                            orderDto.shipped(),
                            Collections.emptyList(),
                            shipmentAddress,
                            Collections.emptySet(),
                            cart
                    );
                    return this.orderRepository.create(order);
                }).onItem().transform(OrderService::mapToDto);
    }

    @WithTransaction
    public Uni<Boolean> cancel(UUID orderId) {
        log.debug("Request to cancel Order: {}", orderId);

        return generateUni_FindById(this.orderRepository, orderId, true)
                .chain(this::cancelOrderBasedOnStatus); // Clean delegation
    }

    private Uni<Boolean> cancelOrderBasedOnStatus(Order order) {
        return switch (order.status) {
            case OrderStatus.DELIVERED, OrderStatus.SHIPPED -> Uni.createFrom()
                    .failure(new IllegalStateException("Order has already been shipped/delivered." +
                            " Try starting a return once delivered"));

            case OrderStatus.PAID -> {
                List<Uni<Boolean>> refundUnis = order.payments.stream()
                        .filter(p -> p != null
                                && (p.status == PaymentStatus.ACCEPTED || p.status == PaymentStatus.PENDING)
                        )
                        .map(payment -> this.paymentService.generateRefund(payment.id))
                        .toList();

                if (refundUnis.isEmpty()) {
                    order.status = OrderStatus.CANCELLED;
                    yield Uni.createFrom().item(Boolean.TRUE);
                } else {
                    yield Uni.join().all(refundUnis).andCollectFailures()
                            .map(results -> {
                                order.status = OrderStatus.CANCELLED;
                                return results.stream().allMatch(Boolean.TRUE::equals);
                            });
                }
            }

            default -> {
                order.status = OrderStatus.CANCELLED;
                yield Uni.createFrom().item(Boolean.TRUE);
            }
        };
    }

    public static Uni<Order> generateUni_FindById(OrderRepository repository, UUID orderId, boolean managed) {
        Uni<Order> generatedUni;
        if (managed)
            generatedUni = repository.findOrderByIdWithOrderItemsManaged(orderId);
        else
            generatedUni = repository.findOrderByIdWithOrderItemsStateless(orderId);

        return generatedUni
                .onItem().ifNull().failWith(() ->
                        new EntityNotFoundException(getOrderNotExistsErrorMsg(orderId))
                );
    }

    public static Uni<Boolean> generateUni_ExistById(OrderRepository repository, UUID orderId, boolean managed) {
        return managed
                ? repository.existByIdManaged(orderId)
                : repository.existByIdStateless(orderId);
    }

    public static String getOrderNotExistsErrorMsg(UUID orderId) {
        return ORDER_NOT_EXISTS_ERROR_MSG + orderId;
    }
}
