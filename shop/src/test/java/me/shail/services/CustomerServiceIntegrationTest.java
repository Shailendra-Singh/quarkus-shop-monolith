package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.common.BaseTest;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CustomerServiceIntegrationTest extends BaseTest {

    @Inject
    CustomerService customerService;

    // --| 1. Create Customer - Tests |---------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
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

    // --| 2. Find Customer - Tests |-----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        // 1. Arrange
        var customers = TestDataFactory.generateMockCustomerDtos(5);

        // 2. Act
        asserter.execute(() ->
                Multi.createFrom().iterable(customers)
                        .onItem().transformToUniAndConcatenate(dto -> customerService.create(dto))
                        .collect().asList()
                        .replaceWithVoid()
        );

        // 3. Assert
        asserter.execute(() ->
                        customerService.findAll()
                                .onItem()
                                .invoke(listOfResults ->
                                {
                                    assertFalse(listOfResults.isEmpty());
                                    assertEquals(customers.size(), listOfResults.size());
                                })

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
                        customerService.create(inputDto).invoke(
                                createdCustomer::set
                ));

        // 2.a Find the customer with id and check
        asserter.execute(() ->
                        customerService
                                .findById(createdCustomer.get().id())
                                .invoke(returnedCustomer -> {
                                    assertNotNull(returnedCustomer);
                                    assertEquals(createdCustomer.get(), returnedCustomer);
                                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindById_WithInvalidId(UniAsserter asserter) {

        // 1.a Prepare the Input id
        UUID nonExistentId = UUID.randomUUID();

        // 2.a Find the customer with id and check
        asserter.assertFailedWith(() ->
                        customerService
                                .findById(nonExistentId)
                                .invoke(Assertions::assertNotNull)
                , throwable -> {
            assertNotNull(throwable);
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("doesn't exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByState(UniAsserter asserter) {
        // 1.a Generate 10 customers
        var customers = TestDataFactory.generateMockCustomerDtos(10);
        List<UUID> deletedCustomerIds = new ArrayList<>();

        // 2.a Create 10 customers
        for (var customer : customers) {
            asserter.execute(() -> customerService
                    .create(customer)
                    .invoke(createdCustomer -> deletedCustomerIds.add(createdCustomer.id()))
            );
        }

        // 2.c Delete 4 customers
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            asserter.execute(() -> customerService.delete(deletedCustomerIds.get(finalI)));
        }

        // 3.a Find the customer with disabled status
        asserter.execute(() ->
                        customerService.findAllByState(false)
                                .invoke(returnedCustomers -> {
                                    // 3.b assert 4 deleted customers
                                    assertNotNull(returnedCustomers);
                                    assertEquals(4, returnedCustomers.size());
                                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAllByState_WhenNoCustomersExist(UniAsserter asserter) {
        asserter.execute(() ->
                        customerService.findAllByState(false)
                                .invoke(returnedCustomers -> {
                                    // 3.b assert 4 deleted customers
                                    assertNotNull(returnedCustomers);
                                    assertEquals(0, returnedCustomers.size());
                                })
        );
    }

    // --| 3. Delete Customer - Tests |---------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testDelete(UniAsserter asserter) {
        // 1. Prepare the Input DTO
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        asserter.execute(() ->
                        customerService.create(inputDto)
                                .chain(createdCustomer -> {
                                    // Assertions
                                    return customerService.delete(createdCustomer.id())
                                            .invoke(Assertions::assertTrue);
                                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testDelete_WhenCustomerDoesNotExist(UniAsserter asserter) {
        // Prepare the Input id
        UUID nonExistentId = UUID.randomUUID();

        asserter.assertFailedWith(() ->
                        // Assertions
                        customerService.delete(nonExistentId)
                                .invoke(Assertions::assertTrue)
                , throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("doesn't exist"));
                });
    }
}