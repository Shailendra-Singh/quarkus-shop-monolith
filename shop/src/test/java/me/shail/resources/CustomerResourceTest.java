package me.shail.resources;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.CustomerService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestHTTPEndpoint(CustomerResource.class)
@JsonTest
public class CustomerResourceTest {

    // --| 1. Create Customer - Tests |---------------------------------------------------------------------------------

    @Test
    public void testCreate() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        // 2. Act
        CustomerDto createdCustomer = given()
                .body(inputDto)
                .post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 3. Assert
        assertNotNull(createdCustomer);
        assertNotNull(createdCustomer.id());
        assertEquals(inputDto.email(), createdCustomer.email());
    }

    // --| 2. Find Customer - Tests |-----------------------------------------------------------------------------------

    @Test
    public void testFindAll() {
        // 1. Arrange
        var inputCustomerDtos = TestDataFactory.generateMockCustomerDtos(5);

        List<CustomerDto> createdCustomers = new ArrayList<>();

        for (var customer : inputCustomerDtos) {
            CustomerDto createdCustomer = given()
                    .body(customer)
                    .post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .as(CustomerDto.class);
            createdCustomers.add(createdCustomer);
        }

        // 2. Act
        List<CustomerDto> fetchedCustomers = given()
                .get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

        // 3. Assert
        assertNotNull(fetchedCustomers);
        assertFalse(fetchedCustomers.isEmpty());
        for (CustomerDto createdCustomer : createdCustomers)
            assertTrue(fetchedCustomers.contains(createdCustomer));
    }

    @Test
    public void testFindById() {

        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 2. Act
        CustomerDto foundCustomer = given()
                .get("/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 3. Assert
        assertEquals(createdCustomer, foundCustomer);
    }

    @Test
    public void testFindById_WithInvalidId() {

        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().get("/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CustomerService.getCustomerDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());

    }

    @Test
    public void testFindAllByState() {
        // 1. Arrange
        var customers = TestDataFactory.generateMockCustomerDtos(10);
        List<UUID> createdCustomerIds = new ArrayList<>();

        for (var customer : customers) {
            CustomerDto createdCustomer = given()
                    .body(customer)
                    .post()
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(CustomerDto.class);
            createdCustomerIds.add(createdCustomer.id());
        }

        List<UUID> deletedCustomerIds = new ArrayList<>();
        // 2. Act
        for (int i = 0; i < 4; i++) {
            given()
                    .delete("/{id}", createdCustomerIds.get(i))
                    .then()
                    .statusCode(RestResponse.StatusCode.NO_CONTENT);
            deletedCustomerIds.add(createdCustomerIds.get(i));
        }

        // 3. Arrange
        List<CustomerDto> returnedCustomers = given().get("/status/disabled")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

        Set<UUID> returnedCustomersSet = returnedCustomers.stream()
                .map(CustomerDto::id).collect(Collectors.toUnmodifiableSet());


        assertNotNull(returnedCustomers);
        assertFalse(returnedCustomers.isEmpty());
        assertTrue(returnedCustomersSet.containsAll(deletedCustomerIds));
    }

    /*
     * Note: `testFindAllByState_WhenNoCustomersExist` cannot be tested
     * Reason: Cross-contamination from other tests
     * */

    // --| 3. Delete Customer - Tests |---------------------------------------------------------------------------------
    @Test
    public void testDelete() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 2. Act
        given()
                .delete("/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        // 3. Assert
        List<CustomerDto> disabledCustomers = given()
                .get("/status/disabled")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

        assertNotNull(disabledCustomers);
        assertTrue(disabledCustomers.contains(createdCustomer));
    }

    @Test
    public void testDelete_WhenCustomerDoesNotExist() {

        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().delete("/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CustomerService.getCustomerDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }
}