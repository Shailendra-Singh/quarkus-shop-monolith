package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.CartDto;
import me.shail.dtos.CustomerDto;
import me.shail.dtos.OrderDto;
import me.shail.dtos.PaymentDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.enums.PaymentStatus;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.OrderService;
import me.shail.services.PaymentService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@JsonTest
public class PaymentResourceTest {
    // --| 1. Create Payment - Tests |----------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().post(PAYMENT_ROOT_URL + "/orders/{orderId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderService.getOrderNotExistsErrorMsg(nonExistentId)))
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
        PaymentDto createdPayment = given()
                .body(orderInputDto)
                .when()
                .post(PAYMENT_ROOT_URL + "/orders/{orderId}", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(PaymentDto.class);

        /* --- 3. Assert --- */

        // Payment created
        assertNotNull(createdPayment);
        assertNotNull(createdPayment.id());
        assertNotNull(createdPayment.paymentReferenceId());
        assertEquals(PaymentStatus.PENDING.name(), createdPayment.status());
        assertEquals(createdOrder.id(), createdPayment.orderId());

        // Check if order now have a payment associated with it
        OrderDto refetchedSameOrder = given()
                .when()
                .get(ORDER_ROOT_URL + "/{id}", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        assertNotNull(refetchedSameOrder);
        assertEquals(createdPayment.id(), refetchedSameOrder.paymentId());

        // Cannot create new payments if pending or accepted payments are already associated with an order
        given()
                .body(orderInputDto)
                .when()
                .post(PAYMENT_ROOT_URL + "/orders/{orderId}", createdOrder.id())
                .then()
                .statusCode(RestResponse.StatusCode.FORBIDDEN)
                .body("message", equalTo(String.format("Cannot pay order %s. Existing payment is %s",
                        createdOrder.id(),
                        createdPayment.status())
                ))
                .body("error", equalTo("Not Permitted"))
                .body("timestamp", notNullValue());
    }

    // --| 2. Find Payment - Tests |------------------------------------------------------------------------------------

    @Test
    public void testFindById_WhenPaymentDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().get(PAYMENT_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(PaymentService.getPaymentNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
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


        OrderDto orderInputDto = TestDataFactory.generateMockOrderDto(createdCart);
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
        PaymentDto foundPayment = given()
                .when()
                .get(PAYMENT_ROOT_URL + "/{id}", createdPaymentId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(PaymentDto.class);

        /* --- 3. Assert --- */
        assertNotNull(foundPayment);
        assertNotNull(foundPayment.id());
        assertNotNull(foundPayment.paymentReferenceId());
        assertEquals(PaymentStatus.PENDING.name(), foundPayment.status());
        assertEquals(createdOrderId, foundPayment.orderId());
    }

    @Test
    public void testFindByPriceRange() {

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
        Set<UUID> allPaymentIdsWithAmountLessThan9 = given()
                .queryParam("maxAmount", BigDecimal.valueOf(9))
                .get(PAYMENT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<PaymentDto>>() {
                }).stream().map(PaymentDto::id).collect(Collectors.toUnmodifiableSet());

        Set<UUID> allPaymentIdsWithAmountLessThan11 = given()
                .queryParam("maxAmount", BigDecimal.valueOf(11))
                .get(PAYMENT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<PaymentDto>>() {
                }).stream().map(PaymentDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */

        // Assert impossible max amount: test order had amount of 10
        assertFalse(allPaymentIdsWithAmountLessThan9.contains(createdPaymentId));
        // Assert always possible max amount: test order had amount of 10
        assertFalse(allPaymentIdsWithAmountLessThan11.isEmpty());
        assertTrue(allPaymentIdsWithAmountLessThan11.contains(createdPaymentId));
    }

    /*
     * Note: `testGenerateRefund` cannot be tested
     * Reason: It's a stub till Transaction Service/Resource is implemented
     * */
}