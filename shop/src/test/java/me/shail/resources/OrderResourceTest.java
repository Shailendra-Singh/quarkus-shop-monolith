package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.enums.OrderStatus;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.CartService;
import me.shail.services.CustomerService;
import me.shail.services.OrderService;
import me.shail.services.PaymentService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@JsonTest
public class OrderResourceTest {
    // --| 1. Create Order - Tests |------------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenCartDoesNotExist() {
        /* --- 1. Arrange --- */
        CartDto mockCartDto = TestDataFactory.generateMockCartDto();
        OrderDto mockOrderDto = TestDataFactory.generateMockOrderDto(mockCartDto);

        /* --- 2. Act --- */
        var response = given().body(mockOrderDto).when().post(ORDER_ROOT_URL);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CartService.getCartDoesNotExistErrorMessage(mockCartDto.id())))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate() {

        /* --- 1. Arrange --- */
        CustomerDto customerInputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(customerInputDto).when()
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


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);

        /* --- 2. Act --- */
        OrderDto createdOrder = given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdOrder);
        assertNotNull(createdOrder.id());
        assertNotNull(createdOrder.cart());
        assertEquals(createdCart, createdOrder.cart());
        assertTrue(createdOrder.orderItems().isEmpty());
        assertEquals(OrderStatus.CREATION.name(), createdOrder.status());
    }

    // --| 2. Find Order - Tests |------------------------------------------------------------------------------------

    @Test
    public void testExistById_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().head(ORDER_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND);
    }

    @Test
    public void testExistById_WhenOrderExist() {
        /* --- 1. Arrange --- */
        CustomerDto customerInputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(customerInputDto).when()
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


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
        OrderDto createdOrder = given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 2. Act --- */
        var response = given().head(ORDER_ROOT_URL + "/{id}", createdOrder.id());

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.OK);
    }

    @Test
    public void testFindById_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().get(ORDER_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderService.getOrderNotExistsErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById_WhenOrderDoesExist() {
        /* --- 1. Arrange --- */
        CustomerDto customerInputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(customerInputDto).when()
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


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
        OrderDto createdOrder = given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 2. Act --- */
        OrderDto result = given()
                .when()
                .get(ORDER_ROOT_URL + "/{id}", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);


        /* --- 3. Assert --- */
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(createdOrder, result);
    }

    @Test
    public void testFindAll() {
        /* --- 1. Arrange --- */
        Set<UUID> createdOrdersIds = new HashSet<>();
        for (CustomerDto inputCustomerDto : TestDataFactory.generateMockCustomerDtos(5)) {
            UUID createdCustomerId = UUID.fromString(given()
                    .body(inputCustomerDto).when()
                    .post(CUSTOMER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            CartDto createdCart = given()
                    .post(CART_ROOT_URL + "/customers/{id}", createdCustomerId)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(CartDto.class);

            OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
            UUID createdOrderId = UUID.fromString(given()
                    .body(orderInputDto)
                    .when()
                    .post(ORDER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            createdOrdersIds.add(createdOrderId);
        }

        /* --- 2. Act --- */
        Set<UUID> allOrderIds = given()
                .get(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<OrderDto>>() {
                }).stream().map(OrderDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */
        assertTrue(allOrderIds.containsAll(createdOrdersIds));
    }

    @Test
    public void testFindAllByCustomer_WhenCustomerDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().get(ORDER_ROOT_URL + "/customers/{customerId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CustomerService.getCustomerDoesNotExistErrorMessage(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindAllByCustomer() {
        /* --- 1. Arrange --- */
        Map<UUID, UUID> customerOrderMap = new HashMap<>();
        for (CustomerDto inputCustomerDto : TestDataFactory.generateMockCustomerDtos(5)) {
            UUID createdCustomerId = UUID.fromString(given()
                    .body(inputCustomerDto).when()
                    .post(CUSTOMER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            CartDto createdCart = given()
                    .post(CART_ROOT_URL + "/customers/{id}", createdCustomerId)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(CartDto.class);

            OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
            UUID createdOrderId = UUID.fromString(given()
                    .body(orderInputDto)
                    .when()
                    .post(ORDER_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));

            customerOrderMap.put(createdCustomerId, createdOrderId);
        }

        /* --- 2. Act --- */
        boolean allTrue = true;
        for (UUID customerId : customerOrderMap.keySet()) {
            Set<UUID> allCustomerOrderIds = given()
                    .get(ORDER_ROOT_URL + "/customers/{customerId}", customerId)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .body()
                    .as(new TypeRef<List<OrderDto>>() {
                    }).stream().map(OrderDto::id).collect(Collectors.toUnmodifiableSet());
            allTrue = allTrue
                    && allCustomerOrderIds.contains(customerOrderMap.get(customerId))
                    && allCustomerOrderIds.size() == 1;
        }

        /* --- 3. Assert --- */
        assertTrue(allTrue);
    }

    @Test
    public void testFindOrderByPaymentId_WhenPaymentDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().get(ORDER_ROOT_URL + "/payments/{paymentId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(PaymentService.getPaymentNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindOrderByPaymentId() {
        /* --- 1. Arrange --- */
        CustomerDto customerInputDto = TestDataFactory.generateMockCustomerDto();

        UUID createdCustomerId = UUID.fromString(given()
                .body(customerInputDto).when()
                .post(CUSTOMER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));


        CartDto createdCart = given()
                .post(CART_ROOT_URL + "/customers/{id}", createdCustomerId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CartDto.class);


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart, BigDecimal.valueOf(10));
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        UUID createdPaymentId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(PAYMENT_ROOT_URL + "/orders/{orderId}", createdOrderId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        /* --- 2. Act --- */
        OrderDto foundOrder = given()
                .when()
                .get(ORDER_ROOT_URL + "/payments/{paymentId}", createdPaymentId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 3. Assert --- */
        assertNotNull(foundOrder);
        assertNotNull(foundOrder.paymentId());
        assertEquals(createdPaymentId, foundOrder.paymentId());
    }

    // --| 3. Delete Order - Tests |--------------------------------------------------------------------------------

    @Test
    public void testCancel_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().post(ORDER_ROOT_URL + "/{orderId}/cancel", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderService.getOrderNotExistsErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCancel() {
        /* --- 1. Arrange --- */
        CustomerDto customerInputDto = TestDataFactory.generateMockCustomerDto();

        CustomerDto createdCustomer = given()
                .body(customerInputDto).when()
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


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
        OrderDto createdOrder = given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 2. Act --- */
        given().post(ORDER_ROOT_URL + "/{orderId}/cancel", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
        OrderDto result = given()
                .when()
                .get(ORDER_ROOT_URL + "/{id}", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED.name(), result.status());
    }
}