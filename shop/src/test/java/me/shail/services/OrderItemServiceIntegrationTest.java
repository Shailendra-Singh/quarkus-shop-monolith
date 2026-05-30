package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.dtos.OrderItemDto;
import me.shail.dtos.ProductDto;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OrderItemServiceIntegrationTest {
    @Inject
    OrderItemService orderItemService;

    @Inject
    ProductService productService;

    @Inject
    OrderService orderService;

    @Inject
    CartService cartService;

    @Inject
    CustomerService customerService;

    // --| 1. Create OrderItem - Tests |--------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WhenOrderAndProductDoesNotExist(UniAsserter asserter) {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemDto mockOrderItemDto = TestDataFactory.generateMockOrderItemDto(productId, orderId);
        asserter.assertFailedWith(() -> orderItemService.create(mockOrderItemDto)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(
                            throwable.getMessage().toLowerCase().contains("order does not exist") ||
                                    throwable.getMessage().toLowerCase().contains("product does not exist")
                    );
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreate(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                .onItem()
                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> inputOrderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    inputOrderDto.set(mockOrderDto);
                })
        );

        // 1.c Create Order associated with the cart
        AtomicReference<OrderDto> createdOrderDto = new AtomicReference<>();
        asserter.execute(() -> orderService.create(inputOrderDto.get()).invoke(createdOrderDto::set));

        // 1.d Create Product 1
        AtomicReference<ProductDto> createdProductDto1 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto1::set));

        // 1.d Create Product 2
        AtomicReference<ProductDto> createdProductDto2 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto2::set));

        // 1.e Create input OrderItem 1
        AtomicReference<OrderItemDto> inputOrderItemDto1 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto1.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto1::set)
        );

        // 1.e Create input OrderItem 2
        AtomicReference<OrderItemDto> inputOrderItemDto2 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto2.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto2::set)
        );

        // 2.a Create OrderItem 1
        asserter.execute(() -> orderItemService.create(inputOrderItemDto1.get()).invoke(createdOrderItem -> {
            assertNotNull(createdOrderItem);
            assertNotNull(createdOrderItem.id());
            assertEquals(createdProductDto1.get().price(), createdOrderItem.unitPrice());
            assertEquals(inputOrderItemDto1.get().quantity(), createdOrderItem.quantity());
            assertEquals(createdProductDto1.get().id(), createdOrderItem.productId());
            assertEquals(createdOrderDto.get().id(), createdOrderItem.orderId());
        }));

        // 2.a Create OrderItem 2
        asserter.execute(() -> orderItemService.create(inputOrderItemDto2.get()));

        // Check calculation
        asserter.execute(() -> orderService.findById(createdOrderDto.get().id())
                .invoke(foundOrder -> {
                    assertNotNull(foundOrder);
                    assertNotNull(foundOrder.orderItems());
                    assertFalse(foundOrder.orderItems().isEmpty());
                    assertEquals(2, foundOrder.orderItems().size());

                    var orderItem1TotalPrice = createdProductDto1.get().price()
                            .multiply(BigDecimal.valueOf(inputOrderItemDto1.get().quantity()));
                    var orderItem2TotalPrice = createdProductDto2.get().price()
                            .multiply(BigDecimal.valueOf(inputOrderItemDto2.get().quantity()));
                    var totalExpectedPrice = orderItem1TotalPrice.add(orderItem2TotalPrice);

                    assertEquals(totalExpectedPrice, foundOrder.price());

                })
        );
    }

    // --| 2. Delete OrderItem - Tests |--------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testDelete_WhenOrderItemDoesNotExist(UniAsserter asserter) {
        UUID orderItemId = UUID.randomUUID();
        asserter.assertFailedWith(() -> orderItemService.deleteById(orderItemId)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("orderitem does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testDeleteById(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                .onItem()
                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> inputOrderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    inputOrderDto.set(mockOrderDto);
                })
        );

        // 1.c Create Order associated with the cart
        AtomicReference<OrderDto> createdOrderDto = new AtomicReference<>();
        asserter.execute(() -> orderService.create(inputOrderDto.get()).invoke(createdOrderDto::set));

        // 1.d Create Product 1
        AtomicReference<ProductDto> createdProductDto1 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto1::set));

        // 1.d Create Product 2
        AtomicReference<ProductDto> createdProductDto2 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto2::set));

        // 1.e Create input OrderItem 1
        AtomicReference<OrderItemDto> inputOrderItemDto1 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto1.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto1::set)
        );

        // 1.e Create input OrderItem 2
        AtomicReference<OrderItemDto> inputOrderItemDto2 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto2.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto2::set)
        );

        AtomicReference<UUID> createdOrderItemDto1Id = new AtomicReference<>();

        // 2.a Create OrderItem 1
        asserter.execute(() -> orderItemService.create(inputOrderItemDto1.get())
                .invoke(createdOrderItemDto1 -> createdOrderItemDto1Id
                        .set(createdOrderItemDto1.id()))
        );

        // 2.a Create OrderItem 2
        asserter.execute(() -> orderItemService.create(inputOrderItemDto2.get()));

        // 3.a Delete one of the order item
        asserter.execute(() -> orderItemService
                .deleteById(createdOrderItemDto1Id.get())
                .invoke(Assertions::assertTrue));

        // Check calculation
        asserter.execute(() -> orderService.findById(createdOrderDto.get().id())
                .invoke(foundOrder -> {
                    assertNotNull(foundOrder);
                    assertNotNull(foundOrder.orderItems());
                    assertFalse(foundOrder.orderItems().isEmpty());
                    assertEquals(1, foundOrder.orderItems().size());

                    var totalExpectedPrice = createdProductDto2.get().price()
                            .multiply(BigDecimal.valueOf(inputOrderItemDto2.get().quantity()));

                    assertEquals(totalExpectedPrice, foundOrder.price());

                })
        );
    }

    // --| 3. Find OrderItem - Tests |----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindAllByOrderId_WhenOrderDoesNotExist(UniAsserter asserter) {
        UUID orderId = UUID.randomUUID();
        asserter.execute(() -> orderItemService.findAllByOrderId(orderId).invoke(result -> {
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }));
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByOrderId(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                .onItem()
                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> inputOrderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    inputOrderDto.set(mockOrderDto);
                })
        );

        // 1.c Create Order associated with the cart
        AtomicReference<OrderDto> createdOrderDto = new AtomicReference<>();
        asserter.execute(() -> orderService.create(inputOrderDto.get()).invoke(createdOrderDto::set));

        // 1.d Create Product 1
        AtomicReference<ProductDto> createdProductDto1 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto1::set));

        // 1.d Create Product 2
        AtomicReference<ProductDto> createdProductDto2 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto2::set));

        // 1.e Create input OrderItem 1
        AtomicReference<OrderItemDto> inputOrderItemDto1 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto1.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto1::set)
        );

        // 1.e Create input OrderItem 2
        AtomicReference<OrderItemDto> inputOrderItemDto2 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto2.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto2::set)
        );

        // 2.a Create OrderItem 1
        asserter.execute(() -> orderItemService.create(inputOrderItemDto1.get()));

        // 2.a Create OrderItem 2
        asserter.execute(() -> orderItemService.create(inputOrderItemDto2.get()));

        // Check calculation
        asserter.execute(() -> orderItemService.findAllByOrderId(createdOrderDto.get().id())
                .invoke(result -> {
                    assertNotNull(result);
                    assertFalse(result.isEmpty());
                    assertEquals(2, result.size());
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindById_WhenOrderItemDoesNotExist(UniAsserter asserter) {
        UUID orderItemId = UUID.randomUUID();
        asserter.assertFailedWith(() -> orderItemService.findById(orderItemId), throwable -> {
            assertNotNull(throwable);
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("orderitem does not exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                .onItem()
                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> inputOrderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    inputOrderDto.set(mockOrderDto);
                })
        );

        // 1.c Create Order associated with the cart
        AtomicReference<OrderDto> createdOrderDto = new AtomicReference<>();
        asserter.execute(() -> orderService.create(inputOrderDto.get()).invoke(createdOrderDto::set));

        // 1.d Create Product 1
        AtomicReference<ProductDto> createdProductDto1 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto1::set));

        // 1.d Create Product 2
        AtomicReference<ProductDto> createdProductDto2 = new AtomicReference<>();
        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto())
                .invoke(createdProductDto2::set));

        // 1.e Create input OrderItem 1
        AtomicReference<OrderItemDto> inputOrderItemDto1 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto1.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto1::set)
        );

        // 1.e Create input OrderItem 2
        AtomicReference<OrderItemDto> inputOrderItemDto2 = new AtomicReference<>();
        asserter.execute(() ->
                Uni.createFrom().item(TestDataFactory.generateMockOrderItemDto(
                        createdProductDto2.get().id(),
                        createdOrderDto.get().id()
                )).invoke(inputOrderItemDto2::set)
        );

        AtomicReference<OrderItemDto> createdOrderItemDto1 = new AtomicReference<>();

        // 2.a Create OrderItem 1
        asserter.execute(() -> orderItemService.create(inputOrderItemDto1.get())
                .invoke(createdOrderItemDto1::set)
        );

        asserter.execute(() -> orderItemService.findById(createdOrderItemDto1.get().id()).invoke(foundOrderItem -> {
            assertNotNull(foundOrderItem);
            assertEquals(createdOrderItemDto1.get(), foundOrderItem);
        }));
    }

}
