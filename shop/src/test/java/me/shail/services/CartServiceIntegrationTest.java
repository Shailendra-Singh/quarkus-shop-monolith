package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@QuarkusTest
public class CartServiceIntegrationTest {
    @Inject
    CartService cartService;

    @Inject
    CustomerRepository customerRepository;

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
                () ->
                        sessionFactory.withStatelessSession(_ ->
                                cartService.getActiveCart(nonExistentCustomer, false)
                        ),
                throwable -> {
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

        // Ensure carts are created
        asserter.execute(() -> sessionFactory.withStatelessSession(_ ->
                cartRepository.listAll().invoke(list -> {
                            assertNotNull(list);
                            assertFalse(list.isEmpty());
                            assertEquals(2, list.size()
                            );
                        }
                )));


        asserter.assertFailedWith(() ->
                        sessionFactory.withStatelessSession(_ ->
                                cartService.getActiveCart(createdCustomerId.get(), false)
                                        .invoke(returnedCart -> {
                                            assertNotNull(returnedCart);
                                            assertNotNull(returnedCart.id());
                                        })
                        ),

                throwable -> {
                    assertEquals(IllegalStateException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("multiple active carts detected"));
                });
    }

    // --| 2. Create Cart - Tests |-------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreateCart_WhenCustomerDoesNotExist(UniAsserter asserter) {
        var nonExistentCustomer = UUID.randomUUID();

        asserter.assertFailedWith(
                () ->
                        sessionFactory.withTransaction(_ ->
                                cartService.create(nonExistentCustomer)
                        ),
                throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("customer does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreateCart_WhenCustomerHasActiveCart(UniAsserter asserter) {
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

        // Ensure carts are created
        asserter.execute(() -> sessionFactory.withStatelessSession(_ ->
                cartRepository.listAll().invoke(list -> {
                            assertNotNull(list);
                            assertFalse(list.isEmpty());
                            assertTrue(list.size() >= 2);
                        }
                )));

        asserter.assertFailedWith(() ->
                        sessionFactory.withTransaction(_ ->
                                cartService.create(createdCustomerId.get())
                                        .invoke(returnedCart -> {
                                            assertNotNull(returnedCart);
                                            assertNotNull(returnedCart.id());
                                        })
                        )
                ,
                throwable -> {
                    assertEquals(IllegalStateException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("active cart already exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreateCart(UniAsserter asserter) {
        // 1.a Prepare the Input DTO

        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<Customer> createdCustomer = new AtomicReference<>();

        // 1.b Create customer
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
                            return customerRepository.create(customer).invoke(createdCustomer::set
                            );
                        }
                )
        );

        asserter.execute(() ->
                sessionFactory.withTransaction(_ ->
                        cartService.create(createdCustomer.get().id)
                                .invoke(returnedCart -> {
                                    assertNotNull(returnedCart);
                                    assertNotNull(returnedCart.id());
                                    assertEquals(returnedCart.customerDto(),
                                            CustomerService.mapToDto(createdCustomer.get()));
                                })
                ));
    }

    // --| 2. Find Cart - Tests |---------------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testFindCart_WhenCartDoesNotExist(UniAsserter asserter) {
        var nonExistentCart = UUID.randomUUID();

        asserter.assertFailedWith(() ->
                sessionFactory.withStatelessSession(_ ->
                        cartService.findById(nonExistentCart)
                ), throwable -> {
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("cart does not exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindCartById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<Customer> createdCustomer = new AtomicReference<>();
        AtomicReference<UUID> createdCartId = new AtomicReference<>();

        // 1.b Create customer
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
                            return customerRepository.create(customer).invoke(createdCustomer::set
                            );
                        }
                )
        );

        asserter.execute(() ->
                sessionFactory.withTransaction(_ ->
                        cartService.create(createdCustomer.get().id)
                                .invoke(returnedCart -> createdCartId.set(returnedCart.id()))
                ));

        asserter.execute(() ->
                sessionFactory.withStatelessSession(_ ->
                        cartService.findById(createdCartId.get()).invoke(returnedCart -> {
                            assertNotNull(returnedCart);
                            assertNotNull(returnedCart.id());
                            assertEquals(returnedCart.id(), createdCartId.get());
                        })
                ));
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

        // Ensure carts are created
        asserter.execute(() -> sessionFactory.withStatelessSession(_ ->
                cartRepository.listAll().invoke(list -> {
                            assertNotNull(list);
                            assertFalse(list.isEmpty());
                        }
                )));


        asserter.execute(() ->
                sessionFactory.withStatelessSession(_ -> cartService.findAllActiveCarts()
                        .invoke(returnedCarts -> {
                            assertNotNull(returnedCarts);
                            assertFalse(returnedCarts.isEmpty());
                        })
                )
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
        asserter.execute(() -> sessionFactory.withStatelessSession(_ ->
                cartService.listAll().invoke(list -> {
                            assertNotNull(list);
                            assertFalse(list.isEmpty());
                        }
                )));
    }

    // --| 3. Delete Cart - Tests |-------------------------------------------------------------------------------------
    @Test
    @RunOnVertxContext
    public void testDelete_WhenCartDoesNotExist(UniAsserter asserter) {
        var nonExistentCart = UUID.randomUUID();

        asserter.assertFailedWith(() ->
                sessionFactory.withTransaction(_ ->
                        cartService.delete(nonExistentCart)
                ), throwable -> {
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("no cart found"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testDelete(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<Customer> createdCustomer = new AtomicReference<>();
        AtomicReference<UUID> createdCartId = new AtomicReference<>();

        // 1.b Create customer
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
                            return customerRepository.create(customer).invoke(createdCustomer::set
                            );
                        }
                )
        );

        // Ensure cart is created
        asserter.execute(() ->
                sessionFactory.withTransaction(_ ->
                        cartService.create(createdCustomer.get().id)
                                .invoke(returnedCart -> {
                                    createdCartId.set(returnedCart.id());
                                    assertEquals(CartStatus.NEW.name(), returnedCart.status());
                                }))
        );

        // Ensure delete succeeds
        asserter.execute(() ->
                sessionFactory.withTransaction(_ ->
                        cartService.delete(createdCartId.get()).invoke(returnedCart -> {
                            assertNotNull(returnedCart);
                            assertTrue(returnedCart);
                        })
                ));

        // Ensure soft-delete changes cart's status to canceled
        asserter.execute(() ->
                sessionFactory.withStatelessSession(_ ->
                        this.cartService.findById(createdCartId.get())
                                .invoke(deletedCart ->
                                        assertEquals(CartStatus.CANCELED.name(),
                                                deletedCart.status())
                                )
                )
        );
    }
}
