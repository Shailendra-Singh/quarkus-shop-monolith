package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.enums.OrderStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class OrderServiceIntegrationTest {

    @Inject
    OrderService orderService;

    @Inject
    CartService cartService;

    @Inject
    CustomerService customerService;

    // --| 1. Create Order - Tests |------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WhenCartDoesNotExist(UniAsserter asserter) {
        // 1.a Create Customer
        CartDto mockCartDto = TestDataFactory.generateMockCartDto();
        OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(mockCartDto);


        asserter.assertFailedWith(() -> orderService.create(mockOrderDto)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("cart does not exist"));
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
        AtomicReference<CartDto> createdCart = new AtomicReference<>();
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    createdCart.set(result);
                                    // 1.c Prepare the Input DTO
                                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                    orderDto.set(mockOrderDto);
                                })
        );


        asserter.execute(() ->
                        orderService.create(orderDto.get())
                                .onItem()
                                .invoke(result -> {
                                            // Assertions
                                            assertNotNull(result);
                                            assertNotNull(result.id());
                                            assertNotNull(result.cart());
                                            assertEquals(createdCart.get(), result.cart());
                                            assertEquals(OrderStatus.CREATION.name(), result.status());
                                        }
                                )
        );
    }

    // --| 2. Find Order - Tests |------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testExistsById_WhenOrderDoesNotExist(UniAsserter asserter) {
        asserter.execute(() -> orderService.existsById(UUID.randomUUID()).invoke(Assertions::assertFalse));
    }

    @Test
    @RunOnVertxContext
    public void testExistsById_WhenOrderExist(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                                .onItem()
                                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    // 1.c Prepare the Input DTO
                                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                    orderDto.set(mockOrderDto);
                                })
        );

        // 1.c Create order
        AtomicReference<OrderDto> createdOrder = new AtomicReference<>();
        asserter.execute(() -> orderService.create(orderDto.get()).invoke(createdOrder::set)
        );

        // 2. Act and Assert
        asserter.execute(() -> orderService.existsById(createdOrder.get().id())
                                .invoke(Assertions::assertTrue)
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindById_WhenOrderDoesNotExist(UniAsserter asserter) {
        // 1.a Fake order id
        UUID fakeOrderId = UUID.randomUUID();

        asserter.assertFailedWith(() -> orderService.findById(fakeOrderId)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("order does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testFindById_WhenOrderDoesExist(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                                .onItem()
                                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    // 1.c Prepare the Input DTO
                                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                    orderDto.set(mockOrderDto);
                                })
        );

        // 1.c Create order
        AtomicReference<OrderDto> createdOrder = new AtomicReference<>();
        asserter.execute(() ->
                        orderService.create(orderDto.get()).invoke(createdOrder::set)
        );

        // 2. Act and Assert
        asserter.execute(() -> orderService.findById(createdOrder.get().id())
                                .invoke(result -> {
                                    assertNotNull(result);
                                    assertNotNull(result.id());
                                    assertEquals(createdOrder.get(), result);
                                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                                .onItem()
                                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    // 1.c Prepare the Input DTO
                                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                    orderDto.set(mockOrderDto);
                                })
        );

        // 1.c Create order
        asserter.execute(() -> orderService.create(orderDto.get())
        );

        // 2. Act and Assert
        asserter.execute(() ->
//                sessionFactory.withStatelessSession(_ ->
                        orderService.listAll()
                                .invoke(result -> {
                                    assertNotNull(result);
                                    assertFalse(result.isEmpty());
                                })
//                )
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByCustomer_WhenCustomerDoesNotExist(UniAsserter asserter) {
        // 1.a Non-Existent Customer
        UUID nonExistentCustomerId = UUID.randomUUID();

        // 2. Act and Assert
        asserter.assertFailedWith(() -> orderService.findAllByCustomer(nonExistentCustomerId)

                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("customer does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByCustomer(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                                .onItem()
                                .invoke(createdCustomer::set)

        );

        // 1.b Create Cart associated with the customer
        List<OrderDto> orderDtoList = new ArrayList<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    // 1.c Create multiple orders
                                    for (int i = 0; i < 3; i++) {
                                        OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                        orderDtoList.add(mockOrderDto);
                                    }
                                })
        );

        // 1.c Create order
        asserter.execute(() -> orderService.create(orderDtoList.getFirst())
                                .chain(_ -> orderService.create(orderDtoList.get(1)))
                                .chain(_ -> orderService.create(orderDtoList.get(2)))

        );

        // 2. Act and Assert
        asserter.execute(() -> orderService.findAllByCustomer(createdCustomer.get().id())
                                .invoke(result -> {
                                    assertNotNull(result);
                                    assertFalse(result.isEmpty());
                                    // Should have 3 orders
                                    assertEquals(3, result.size());
                                })
        );
    }

    // --| 3. Delete Order - Tests |--------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testCancel_WhenOrderDoesNotExist(UniAsserter asserter) {
        // 1.a Non-Existent Order
        UUID nonExistentOrderId = UUID.randomUUID();

        // 2. Act and Assert
        asserter.assertFailedWith(() -> orderService.cancel(nonExistentOrderId)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("order does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCancel(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() ->
                        customerService.create(inputDto)
                                .onItem()
                                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(result -> {
                                    // 1.c Prepare the Input DTO
                                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                                    orderDto.set(mockOrderDto);
                                })
        );

        // 1.c Create order
        AtomicReference<OrderDto> createdOrder = new AtomicReference<>();
        asserter.execute(() -> orderService.create(orderDto.get()).invoke(createdOrder::set)
        );

        // 2. Act and Assert
        asserter.execute(() -> orderService.cancel(createdOrder.get().id())
                                .invoke(result -> {
                                    assertNotNull(result);
                                    assertTrue(result);
                                })
        );

        // Check if order status is canceled
        asserter.execute(() -> orderService.findById(createdOrder.get().id())
                .invoke(result -> {
                    assertNotNull(result);
                    assertEquals(OrderStatus.CANCELLED.name(), result.status());
                })
        );
    }
}
