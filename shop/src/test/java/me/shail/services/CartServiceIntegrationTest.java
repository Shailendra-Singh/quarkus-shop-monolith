package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.services.common.BaseTest;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.Cart;
import me.shail.models.Customer;
import me.shail.models.enums.CartStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.CustomerRepository;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@QuarkusTest
public class CartServiceIntegrationTest extends BaseTest {
    @Inject
    CartService cartService;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    CustomerService customerService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    CartRepository cartRepository;

    // --| 1. Active Cart - Tests |-------------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testGetActiveCart_WhenCustomerDoesNotExist(UniAsserter asserter) {
        var nonExistentCustomer = UUID.randomUUID();

        asserter.assertFailedWith(
                () -> cartService.getActiveCart(nonExistentCustomer)
                , throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("customer does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testGetActiveCart_WhenCustomerHasMultipleCarts(UniAsserter asserter) {
        // 1.a Prepare the Input DTO

        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<UUID> createdCustomerId = new AtomicReference<>();

        // 1.b Create customer and carts
        asserter.execute(() ->
                sessionFactory.withTransaction(_ -> {
                            Customer customer = new Customer(
                                    inputDto.firstName(),
                                    inputDto.lastName(),
                                    inputDto.email(),
                                    inputDto.telephone(),
                                    Collections.emptySet(),
                                    true
                            );
                            return customerRepository.create(customer).invoke(_createdCustomer ->
                                    createdCustomerId.set(_createdCustomer.id)
                            );
                        }

                ).chain(createdCustomer -> sessionFactory.withTransaction(_ -> {
                            Cart cart1 = new Cart(createdCustomer, CartStatus.NEW);
                            Cart cart2 = new Cart(createdCustomer, CartStatus.NEW);
                            return cartRepository.create(cart1)
                                    .chain(() -> cartRepository.create(cart2));
                        }
                )).replaceWith(true)
        );

        asserter.assertFailedWith(() ->
                                cartService.getActiveCart(createdCustomerId.get())
                                        .invoke(returnedCart -> {
                                            assertNotNull(returnedCart);
                                            assertNotNull(returnedCart.id());
                                        }), throwable -> {
                    assertEquals(IllegalStateException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("multiple active carts detected"));
                });
    }

    // --| 2. Create Cart - Tests |-------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WhenCustomerDoesNotExist(UniAsserter asserter) {
        var nonExistentCustomer = UUID.randomUUID();

        asserter.assertFailedWith(
                () -> cartService.create(nonExistentCustomer), throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("customer does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreate_WhenCustomerHasActiveCart(UniAsserter asserter) {
        // 1.a Prepare the Input DTO

        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<UUID> createdCustomerId = new AtomicReference<>();

        // 1.b Create customer and carts
        asserter.execute(() ->
                sessionFactory.withTransaction(_ -> {
                            Customer customer = new Customer(
                                    inputDto.firstName(),
                                    inputDto.lastName(),
                                    inputDto.email(),
                                    inputDto.telephone(),
                                    Collections.emptySet(),
                                    true
                            );
                            return customerRepository.create(customer).invoke(_createdCustomer ->
                                    createdCustomerId.set(_createdCustomer.id)
                            );
                        }

                ).chain(createdCustomer -> sessionFactory.withTransaction(_ -> {
                            Cart cart1 = new Cart(createdCustomer, CartStatus.NEW);
                            Cart cart2 = new Cart(createdCustomer, CartStatus.CANCELED);
                            return cartRepository.create(cart1)
                                    .chain(() -> cartRepository.create(cart2));
                        }
                )).replaceWith(true)
        );

        asserter.assertFailedWith(() -> cartService.create(createdCustomerId.get())
                                        .invoke(returnedCart -> {
                                            assertNotNull(returnedCart);
                                            assertNotNull(returnedCart.id());
                                        }), throwable -> {
                    assertEquals(IllegalStateException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("active cart already exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreate(UniAsserter asserter) {
        // 1.a Prepare the Input DTO

        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        // 1.b Create customer
        asserter.execute(() ->
                customerService.create(inputDto).invoke(createdCustomer::set)
        );

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(returnedCart -> {
                                    assertNotNull(returnedCart);
                                    assertNotNull(returnedCart.id());
                                    assertEquals(returnedCart.customerDto(),
                                            createdCustomer.get());
                                })
        );
    }

    // --| 2. Find Cart - Tests |---------------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testFind_WhenCartDoesNotExist(UniAsserter asserter) {
        var nonExistentCart = UUID.randomUUID();

        asserter.assertFailedWith(() -> cartService.findById(nonExistentCart), throwable -> {
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("cart does not exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();
        AtomicReference<UUID> createdCartId = new AtomicReference<>();

        // 1.b Create customer
        asserter.execute(() -> customerService.create(inputDto).invoke(createdCustomer::set));

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                                .invoke(returnedCart -> createdCartId.set(returnedCart.id()))
        );

        asserter.execute(() -> cartService.findById(createdCartId.get()).invoke(returnedCart -> {
                            assertNotNull(returnedCart);
                            assertNotNull(returnedCart.id());
                            assertEquals(returnedCart.id(), createdCartId.get());
                        })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAllActiveCarts(UniAsserter asserter) {
        // 1.a Prepare the Input DTO
        var inputDto = TestDataFactory.generateMockCustomerDto();

        // 1.b Create customer and carts
        asserter.execute(() ->
                sessionFactory.withTransaction(_ -> {
                            Customer customer = new Customer(
                                    inputDto.firstName(),
                                    inputDto.lastName(),
                                    inputDto.email(),
                                    inputDto.telephone(),
                                    Collections.emptySet(),
                                    true
                            );
                            return customerRepository.create(customer);
                        }

                ).chain(createdCustomer -> sessionFactory.withTransaction(_ -> {
                            Cart cart1 = new Cart(createdCustomer, CartStatus.NEW);
                            Cart cart2 = new Cart(createdCustomer, CartStatus.CANCELED);
                            Cart cart3 = new Cart(createdCustomer, CartStatus.CONFIRMED);
                            return cartRepository.create(cart1)
                                    .chain(() -> cartRepository.create(cart2))
                                    .chain(() -> cartRepository.create(cart3));
                        }
                )).replaceWith(true)
        );

        asserter.execute(() ->
                cartService.findAllActiveCarts()
                        .invoke(returnedCarts -> {
                            assertNotNull(returnedCarts);
                            assertEquals(1, returnedCarts.size());
                        })
        );
    }

    @Test
    @RunOnVertxContext
    public void testListAll(UniAsserter asserter) {
        // 1.a Prepare the Input DTO

        var inputDto = TestDataFactory.generateMockCustomerDto();

        // 1.b Create customer and carts
        asserter.execute(() ->
                sessionFactory.withTransaction(_ -> {
                            Customer customer = new Customer(
                                    inputDto.firstName(),
                                    inputDto.lastName(),
                                    inputDto.email(),
                                    inputDto.telephone(),
                                    Collections.emptySet(),
                                    true
                            );
                            return customerRepository.create(customer);
                        }

                ).chain(createdCustomer -> sessionFactory.withTransaction(_ -> {
                            Cart cart1 = new Cart(createdCustomer, CartStatus.NEW);
                            Cart cart2 = new Cart(createdCustomer, CartStatus.CANCELED);
                            Cart cart3 = new Cart(createdCustomer, CartStatus.CONFIRMED);
                            return cartRepository.create(cart1)
                                    .chain(() -> cartRepository.create(cart2))
                                    .chain(() -> cartRepository.create(cart3));
                        }
                )).replaceWith(true)
        );

        // Ensure carts are created
        asserter.execute(() ->
                cartService.listAll().invoke(list -> {
                            assertNotNull(list);
                    assertEquals(3, list.size());
                        }
                )
        );
    }

    // --| 3. Delete Cart - Tests |-------------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testDelete_WhenCartDoesNotExist(UniAsserter asserter) {
        var nonExistentCart = UUID.randomUUID();

        asserter.assertFailedWith(() ->
                        cartService.delete(nonExistentCart)
                , throwable -> {
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("no cart found"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testDelete(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();
        AtomicReference<UUID> createdCartId = new AtomicReference<>();

        // 1.b Create customer
        asserter.execute(() -> customerService.create(inputDto).invoke(createdCustomer::set));

        asserter.execute(() -> cartService.create(createdCustomer.get().id())
                .invoke(returnedCart -> createdCartId.set(returnedCart.id()))
        );

        // Ensure delete succeeds
        asserter.execute(() ->
                        cartService.delete(createdCartId.get()).invoke(returnedCart -> {
                            assertNotNull(returnedCart);
                            assertTrue(returnedCart);
                        })
        );

        // Ensure soft-delete changes cart's status to canceled
        asserter.execute(() ->
                        this.cartService.findById(createdCartId.get())
                                .invoke(deletedCart ->
                                        assertEquals(CartStatus.CANCELED.name(),
                                                deletedCart.status())
                                )
        );
    }
}