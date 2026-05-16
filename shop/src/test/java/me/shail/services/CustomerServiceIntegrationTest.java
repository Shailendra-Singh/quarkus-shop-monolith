package me.shail.services;

import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.repositories.CustomerRepository;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CustomerServiceIntegrationTest {

    @Inject
    CustomerService customerService;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @TestReactiveTransaction
    public void testCreate(UniAsserter asserter) {
        // 1. Prepare the Input DTO
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        asserter.execute(() ->
                customerService.create(inputDto)
                        .onItem()
                        .invoke(result -> {
                                    // Assertions
                                    assertNotNull(result);
                                    assertNotNull(result.id());
                                    assertEquals(inputDto.email(), result.email());
                                }
                        )
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        // 1. Arrange
        var customers = TestDataFactory.generateMockCustomerDtos(5);

        AtomicReference<List<CustomerDto>> createdCustomers = new AtomicReference<>();

        // 2. Act
        asserter.execute(() ->
                Multi.createFrom().iterable(customers)
                        .onItem().transformToUniAndConcatenate(dto -> customerService.create(dto))
                        .collect().asList()
                        .replaceWithVoid()
        );

        // 3. Assert
        asserter.execute(() ->
                sessionFactory.withStatelessSession(session ->
                        customerService.findAll()
                                .onItem().invoke(listOfResults -> {
                                    createdCustomers.set(listOfResults);
                                    assertEquals(customers.size(), listOfResults.size());
                                })
                )
        );

        // Clean up
        asserter.execute(() ->
                sessionFactory.withSession(session ->
                        Multi.createFrom().iterable(createdCustomers.get())
                                .onItem()
                                .transformToUniAndConcatenate(customer ->
                                        customerRepository.deleteById(customer.id()
                                        )
                                ).collect().asList()
                )
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {

        // 1.a Prepare the Input DTO
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();
        AtomicReference<CustomerDto> createdCustomer = new AtomicReference<>();

        // 1.b Create customer
        asserter.execute(() ->
                sessionFactory.withSession(session ->
                        customerService.create(inputDto).invoke(
                                createdCustomer::set
                        )
                ));

        // 2.a Find the customer with id and check
        asserter.execute(() ->
                sessionFactory.withStatelessSession(session ->
                        customerService
                                .findById(createdCustomer.get().id())
                                .invoke(returnedCustomer -> {
                                    assertNotNull(returnedCustomer);
                                    assertEquals(createdCustomer.get(), returnedCustomer);
                                })
                ));
    }

    @Test
    @RunOnVertxContext
    public void testFindById_WithInvalidId(UniAsserter asserter) {

        // 1.a Prepare the Input id
        UUID nonExistentId = UUID.randomUUID();

        // 2.a Find the customer with id and check
        asserter.assertFailedWith(() ->
                sessionFactory.withStatelessSession(session ->
                        customerService
                                .findById(nonExistentId)
                                .invoke(Assertions::assertNotNull)
                ), throwable -> {
            assertNotNull(throwable);
            assertEquals(NoSuchElementException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("doesn't exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByState(UniAsserter asserter) {
        //  1.a Generate 10 customers
        var customers = TestDataFactory.generateMockCustomerDtos(10);

        asserter.execute(() ->
                sessionFactory.withSession(session -> {

                    // 2.a Create 10 customers
                    List<Uni<CustomerDto>> createdCustomerUnis = new ArrayList<>();
                    for (var customer : customers) {
                        createdCustomerUnis.add(customerService.create(customer));
                    }

                    return Uni.join().all(createdCustomerUnis).andFailFast().chain(listOfResults -> {
                        // 2.b Delete 4 customers
                        List<Uni<Boolean>> deleteCustomerUnis = new ArrayList<>();
                        for (int i = 0; i < listOfResults.size(); i++) {
                            var customer = listOfResults.get(i);
                            // 2.c Create 4 delete streams
                            if (i < 4)
                                deleteCustomerUnis.add(customerService.delete(customer.id()));
                        }

                        // 2.d find customers by state: enabled = false
                        return Uni.join().all(deleteCustomerUnis).andFailFast();

                    });
                }));

        // 3.a Find the customer with disabled status
        asserter.execute(() ->
                sessionFactory.withStatelessSession(session ->
                        customerService.findAllByState(false)
                                .invoke(returnedCustomers -> {
                                    // 3.b assert 4 deleted customers
                                    assertNotNull(returnedCustomers);
                                    assertEquals(4, returnedCustomers.size());
                                })
                ));
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByState_WhenNoCustomersExist(UniAsserter asserter) {
        asserter.execute(() ->
                sessionFactory.withStatelessSession(session ->
                        customerService.findAllByState(false)
                                .invoke(returnedCustomers -> {
                                    // 3.b assert 4 deleted customers
                                    assertNotNull(returnedCustomers);
                                    assertEquals(0, returnedCustomers.size());
                                })
                ));
    }

    @Test
    @RunOnVertxContext
    public void testDelete(UniAsserter asserter) {
        // 1. Prepare the Input DTO
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        asserter.execute(() -> sessionFactory.withSession(session ->
                        customerService.create(inputDto)
                                .chain(createdCustomer -> {
                                    // Assertions
                                    return customerService.delete(createdCustomer.id())
                                            .invoke(Assertions::assertTrue);
                                })
                )
        );
    }

    @Test
    @RunOnVertxContext
    public void testDelete_WhenCustomerDoesNotExist(UniAsserter asserter) {
        // Prepare the Input id
        UUID nonExistentId = UUID.randomUUID();

        asserter.assertFailedWith(() -> sessionFactory.withSession(session ->
                        // Assertions
                        customerService.delete(nonExistentId)
                                .invoke(Assertions::assertTrue)
                )
                , throwable -> {
                    assertEquals(NoSuchElementException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("doesn't exist"));
                });
    }
}