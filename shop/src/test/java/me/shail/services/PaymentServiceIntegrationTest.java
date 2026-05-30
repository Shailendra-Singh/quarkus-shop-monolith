package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PaymentServiceIntegrationTest {

    @Inject
    OrderService orderService;
    @Inject
    PaymentService paymentService;
    @Inject
    CustomerService customerService;
    @Inject
    CartService cartService;

    // --| 1. Create Payment - Tests |----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WhenOrderDoesNotExist(UniAsserter asserter) {
        UUID fakeOrderId = UUID.randomUUID();

        asserter.assertFailedWith(() -> paymentService.create(fakeOrderId)
                , throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("order does not exist"));
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
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        // Crate Cart
        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    // 1.c Prepare the Input DTO
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    orderDto.set(mockOrderDto);
                })
        );

        // Created Order
        AtomicReference<UUID> createdOrderId = new AtomicReference<>();

        asserter.execute(() -> orderService.create(orderDto.get()).onItem()
                .invoke(createdOrder -> createdOrderId.set(createdOrder.id()))
        );

        AtomicReference<UUID> createdPaymentId = new AtomicReference<>();

        asserter.execute(() -> paymentService.create(createdOrderId.get()).invoke(paymentDto -> {
            assertNotNull(paymentDto);
            assertNotNull(paymentDto.id());
            createdPaymentId.set(paymentDto.id());

            assertNotNull(paymentDto.paymentReferenceId());
            assertEquals(PaymentStatus.PENDING.name(), paymentDto.status());
            assertEquals(createdOrderId.get(), paymentDto.orderId());
        }));

        asserter.execute(() -> orderService.findById(createdOrderId.get()).onItem().invoke(order -> {
            assertNotNull(order);
            assertEquals(createdPaymentId.get(), order.paymentId());
        }));

        // Cannot create new payments if pending or accepted payments are already associated with an order
        asserter.assertFailedWith(() -> paymentService.create(createdOrderId.get()), throwable -> {
            assertNotNull(throwable);
            assertEquals(OperationNotSupportedException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("cannot pay order"));
        });
    }

    // --| 2. Find Payment - Tests |------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindById_WhenPaymentDoesNotExist(UniAsserter asserter) {
        asserter.assertFailedWith(() -> paymentService.findById(UUID.randomUUID()), throwable -> {
            assertNotNull(throwable);
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("payment does not exist"));
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
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        // Crate Cart
        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    // 1.c Prepare the Input DTO
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result);
                    orderDto.set(mockOrderDto);
                })
        );

        // Created Order
        AtomicReference<UUID> createdOrderId = new AtomicReference<>();

        asserter.execute(() -> orderService.create(orderDto.get()).onItem()
                .invoke(createdOrder -> createdOrderId.set(createdOrder.id()))
        );

        // Create Payment
        AtomicReference<UUID> createdPaymentId = new AtomicReference<>();
        asserter.execute(() -> paymentService.create(createdOrderId.get())
                .invoke(paymentDto -> createdPaymentId.set(paymentDto.id()))
        );

        // Assert
        asserter.execute(() -> paymentService.findById(createdPaymentId.get()).invoke(paymentDto -> {
            assertNotNull(paymentDto);
            assertNotNull(paymentDto.id());
            assertNotNull(paymentDto.paymentReferenceId());
            assertEquals(PaymentStatus.PENDING.name(), paymentDto.status());
            assertEquals(createdOrderId.get(), paymentDto.orderId());
        }));
    }

    @Test
    @RunOnVertxContext
    public void testFindByPriceRange(UniAsserter asserter) {
        // 1.a Create Customer
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        asserter.execute(() -> customerService.create(inputDto)
                .onItem()
                .invoke(createdCustomer::set)
        );

        // 1.b Create Cart associated with the customer
        AtomicReference<OrderDto> orderDto = new AtomicReference<>();

        // Crate Cart
        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(result -> {
                    // 1.c Prepare the Input DTO
                    OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(result, BigDecimal.valueOf(10));
                    orderDto.set(mockOrderDto);
                })
        );

        // Created Order
        AtomicReference<UUID> createdOrderId = new AtomicReference<>();

        asserter.execute(() -> orderService.create(orderDto.get()).onItem()
                .invoke(createdOrder -> createdOrderId.set(createdOrder.id()))
        );

        // Create Payment
        asserter.execute(() -> paymentService.create(createdOrderId.get()));

        // Assert impossible max amount: negative amount
        asserter.execute(() -> paymentService.findByPriceRange(-1d).invoke(paymentDtos -> {
            assertNotNull(paymentDtos);
            assertTrue(paymentDtos.isEmpty());
        }));

        // Assert always possible max amount: test order has maximum of 1000
        asserter.execute(() -> paymentService.findByPriceRange(1001d).invoke(paymentDtos -> {
            assertNotNull(paymentDtos);
            assertFalse(paymentDtos.isEmpty());
        }));
    }
}
