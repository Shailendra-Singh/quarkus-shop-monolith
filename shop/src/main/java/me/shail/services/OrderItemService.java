package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.OrderItemDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Order;
import me.shail.models.OrderItem;
import me.shail.models.Product;
import me.shail.repositories.OrderItemRepository;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class OrderItemService {

    @Inject
    OrderItemRepository orderItemRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    public static OrderItemDto mapToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.id,
                orderItem.unitPrice,
                orderItem.quantity,
                orderItem.product.id,
                orderItem.order.id
        );
    }

    public static OrderItemDto mapToDto(OrderItem orderItem, UUID productId, UUID orderId) {
        return new OrderItemDto(
                orderItem.id,
                orderItem.unitPrice,
                orderItem.quantity,
                productId,
                orderId
        );
    }

    public static Uni<OrderItem> generateUni_FindById(OrderItemRepository repository,
                                                      UUID orderItemId,
                                                      boolean managed) {
        Uni<OrderItem> generatedUni;
        if (managed)
            generatedUni = repository.findByIdManaged(orderItemId);
        else
            generatedUni = repository.findByIdStateless(orderItemId);

        return generatedUni.onItem()
                .ifNull()
                .failWith(() -> new EntityNotFoundException("OrderItem does not exist. Id: " + orderItemId));
    }

    @WithCustomStatelessSession
    public Uni<OrderItemDto> findById(UUID orderItemId) {
        log.debug("Request to get OrderItem: {}", orderItemId);
        return generateUni_FindById(this.orderItemRepository, orderItemId, false)
                .map(OrderItemService::mapToDto);
    }

    @WithTransaction
    public Uni<OrderItemDto> create(OrderItemDto orderItemDto) {
        log.debug("Request to create OrderItem: {}", orderItemDto);
        Uni<Order> uni_findOrderById = OrderService.generateUni_FindById
                (
                        this.orderRepository,
                        orderItemDto.orderId(),
                        true
                );

        Uni<Product> uni_findProductById = ProductService.generateUni_FindById
                (
                        this.productRepository,
                        orderItemDto.productId(),
                        true
                );

        return uni_findOrderById
                .chain(order -> uni_findProductById.chain(product -> {
                    // 1. Get Order and Product
                    OrderItem orderItem = new OrderItem(product.price, orderItemDto.quantity(), product, order);
                    // 2. Update Order State
                    order.price = order.price.add(
                            product.price.multiply(BigDecimal.valueOf(orderItemDto.quantity()))
                    );
                    order.price = order.price.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : order.price;
                    // 3. Persist
                    return this.orderItemRepository.create(orderItem);
                }))
                .map(result -> OrderItemService.mapToDto(result,
                        orderItemDto.productId(),
                        orderItemDto.orderId())
                );
    }

    @WithTransaction
    public Uni<Boolean> deleteById(UUID orderItemId) {
        log.debug("Request to delete OrderItem: {}", orderItemId);
        return generateUni_FindById(this.orderItemRepository, orderItemId, true)
                .chain(orderItem -> {
                    Order order = orderItem.order;
                    // reduce the price due to orderItem's removal
                    BigDecimal reduction = orderItem.unitPrice.multiply(BigDecimal.valueOf(orderItem.quantity));
                    order.price = order.price.subtract(reduction);

                    return this.orderItemRepository.deleteById(orderItemId);
                });
    }

    @WithCustomStatelessSession
    public Uni<List<OrderItemDto>> findAllByOrderId(UUID orderId) {
        log.debug("Request to get all OrderItems of OrderId {}", orderId);
        return this.orderItemRepository
                .findAllByOrderId(orderId)
                .map(items -> items.stream()
                        .map(OrderItemService::mapToDto)
                        .toList());
    }
}