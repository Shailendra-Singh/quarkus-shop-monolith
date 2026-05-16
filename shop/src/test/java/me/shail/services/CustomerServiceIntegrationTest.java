package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class CustomerServiceIntegrationTest {

    @Inject
    CustomerService customerService;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testCreate(UniAsserter asserter) {
        // 1. Prepare the Input DTO
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        asserter.execute(() -> sessionFactory.withSession(session -> customerService.create(inputDto).onItem().invoke(result -> {
            // Assertions
            assertNotNull(result);
            assertNotNull(result.id());
            assertEquals(inputDto.email(), result.email());
        })));
    }

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        // 1. Arrange
        var customers = TestDataFactory.generateMockCustomerDtos(5);

        // 2. Act & Assert
        asserter.execute(() ->
                // Open ONE session at the very top level for the entire block
                sessionFactory.withSession(session -> {

                    // Build the Unis INSIDE the session block so they can inherit the active context
                    List<Uni<CustomerDto>> customerUnis = new ArrayList<>();
                    for (var customer : customers) {
                        customerUnis.add(customerService.create(customer));
                    }

                    // Join them. This works because they all branch off the same initial session context
                    return Uni.join().all(customerUnis).andFailFast().invoke(listOfResults -> {
                        // 3. Assert
                        assertEquals(customers.size(), listOfResults.size());
                    });
                }));
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
}