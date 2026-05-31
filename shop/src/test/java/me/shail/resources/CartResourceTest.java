package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.enums.CartStatus;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.CartService;
import me.shail.services.CustomerService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.CART_ROOT_URL;
import static me.shail.helpers.Constants.CUSTOMER_ROOT_URL;
import static me.shail.services.CartService.ACTIVE_CART_EXIST_ERROR_MSG;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@JsonTest
public class CartResourceTest {

    // --| 1. Active Cart - Tests |-------------------------------------------------------------------------------------

    @Test

    public void testGetActiveCart_WhenCustomerDoesNotExist() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().get(CART_ROOT_URL + "/customers/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CustomerService.getCustomerDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    /*
     * Note: `testGetActiveCart_WhenCustomerHasMultipleCarts` cannot be tested
     * Reason: Service layer and database constraints mandates it cannot happen
     * */

    // --| 2. Create Cart - Tests |-------------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenCustomerDoesNotExist() {

        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().post(CART_ROOT_URL + "/customers/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CustomerService.getCustomerDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate_WhenCustomerHasActiveCart() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post(CUSTOMER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 2. Act
        CartDto _ = given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        // 3. Assert
        given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.BAD_REQUEST)
                .body("message", equalTo(ACTIVE_CART_EXIST_ERROR_MSG))
                .body("error", equalTo("Bad Request"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post(CUSTOMER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        // 2. Act
        CartDto returnedCart = given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        // 3. Assert
        assertNotNull(returnedCart);
        assertNotNull(returnedCart.id());
        assertEquals(returnedCart.customerDto(), createdCustomer);
    }

    // --| 3. Find Cart - Tests |---------------------------------------------------------------------------------------

    @Test
    public void testFind_WhenCartDoesNotExist() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().get(CART_ROOT_URL + "/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CartService.getCartDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post(CUSTOMER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        CartDto createdCart = given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        // 2. Act
        CartDto returnedCart = given()
                .get(CART_ROOT_URL + "/{id}", createdCart.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        // 3. Assert
        assertNotNull(returnedCart);
        assertNotNull(returnedCart.id());
        assertEquals(returnedCart, createdCart);
    }

    @Test
    public void testFindAllActiveCarts() {
        // 1. Arrange
        Set<UUID> createdCarts = new HashSet<>();
        for (CustomerDto inputCustomerDto : TestDataFactory.generateMockCustomerDtos(5)) {
            CustomerDto createdCustomer = given()
                    .body(inputCustomerDto).when()
                    .post(CUSTOMER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(CustomerDto.class);

            UUID createdCartId = UUID.fromString(given()
                    .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            createdCarts.add(createdCartId);
        }

        // 2. Act
        Set<UUID> allCartIds = given()
                .get(CART_ROOT_URL + "/active")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<CartDto>>() {
                }).stream().map(CartDto::id).collect(Collectors.toUnmodifiableSet());

        // 3. Assert
        assertTrue(allCartIds.containsAll(createdCarts));
    }

    @Test
    public void testListAll() {
        // 1. Arrange
        Set<UUID> createdCarts = new HashSet<>();
        for (CustomerDto inputCustomerDto : TestDataFactory.generateMockCustomerDtos(5)) {
            CustomerDto createdCustomer = given()
                    .body(inputCustomerDto).when()
                    .post(CUSTOMER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(CustomerDto.class);

            UUID createdCartId = UUID.fromString(given()
                    .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            createdCarts.add(createdCartId);
        }

        // 2. Act
        Set<UUID> allCartIds = given()
                .get(CART_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<CartDto>>() {
                }).stream().map(CartDto::id).collect(Collectors.toUnmodifiableSet());

        // 3. Assert
        assertTrue(allCartIds.containsAll(createdCarts));
    }

    // --| 4. Delete Cart - Tests |-------------------------------------------------------------------------------------

    @Test
    public void testDelete_WhenCartDoesNotExist() {
        // 1. Arrange
        UUID nonExistentId = UUID.randomUUID();

        // 2. Act
        var response = given().delete(CART_ROOT_URL + "/{id}", nonExistentId);

        // 3. Assert
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CartService.getCartDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testDelete() {
        // 1. Arrange
        CustomerDto inputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(inputDto).when()
                .post(CUSTOMER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CustomerDto.class);

        CartDto createdCart = given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomer.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        // 2. Act
        given()
                .delete(CART_ROOT_URL + "/{id}", createdCart.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        // 3. Assert
        CartDto returnedCart = given()
                .get(CART_ROOT_URL + "/{id}", createdCart.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);

        assertNotNull(returnedCart);
        assertEquals(createdCart.id(), returnedCart.id());
        assertEquals(CartStatus.CANCELED.name(), returnedCart.status());
    }
}