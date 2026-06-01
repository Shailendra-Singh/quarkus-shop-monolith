package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.*;
import me.shail.helpers.data.TestDataFactory;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.OrderItemService;
import me.shail.services.OrderService;
import me.shail.services.ProductService;
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
public class OrderItemResourceTest {
    // --| 1. Create OrderItem - Tests |--------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentOrderId = UUID.randomUUID();
        UUID nonExistentProductId = UUID.randomUUID();

        OrderItemDto inputOrderItemDto = TestDataFactory
                .generateMockOrderItemDto(nonExistentProductId, nonExistentOrderId);

        /* --- 2. Act --- */
        var response = given().body(inputOrderItemDto).when().post(ORDER_ITEM_ROOT_URL);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderService.getOrderNotExistsErrorMsg(nonExistentOrderId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate_WhenProductDoesNotExist() {
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
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        UUID nonExistentProductId = UUID.randomUUID();

        OrderItemDto inputOrderItemDto = TestDataFactory
                .generateMockOrderItemDto(nonExistentProductId, createdOrderId);

        /* --- 2. Act --- */
        var response = given().body(inputOrderItemDto).when().post(ORDER_ITEM_ROOT_URL);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(nonExistentProductId)))
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
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        ProductDto createdProductDto1 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        ProductDto createdProductDto2 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 2. Act --- */
        OrderItemDto inputOrderItemDto1 = TestDataFactory.generateMockOrderItemDto(createdProductDto1.id(),
                createdOrderId);
        OrderItemDto createdOrderItemDto1 = given()
                .body(inputOrderItemDto1)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        OrderItemDto inputOrderItemDto2 = TestDataFactory.generateMockOrderItemDto(createdProductDto2.id(),
                createdOrderId);
        given()
                .body(inputOrderItemDto2)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        OrderDto updatedOrder = given()
                .when()
                .get(ORDER_ROOT_URL + "/{id}", createdOrderId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdOrderItemDto1);
        assertNotNull(createdOrderItemDto1.id());
        assertEquals(createdProductDto1.price(), createdOrderItemDto1.unitPrice());
        assertEquals(inputOrderItemDto1.quantity(), createdOrderItemDto1.quantity());
        assertEquals(createdProductDto1.id(), createdOrderItemDto1.productId());
        assertEquals(createdOrderId, createdOrderItemDto1.orderId());

        assertNotNull(updatedOrder);
        assertNotNull(updatedOrder.orderItems());
        assertFalse(updatedOrder.orderItems().isEmpty());
        assertEquals(2, updatedOrder.orderItems().size());

        var orderItem1TotalPrice = createdProductDto1.price()
                .multiply(BigDecimal.valueOf(inputOrderItemDto1.quantity()));
        var orderItem2TotalPrice = createdProductDto2.price()
                .multiply(BigDecimal.valueOf(inputOrderItemDto2.quantity()));
        var totalExpectedPrice = orderItem1TotalPrice.add(orderItem2TotalPrice);

        assertEquals(totalExpectedPrice, updatedOrder.price());
    }

    // --| 2. Delete OrderItem - Tests |--------------------------------------------------------------------------------

    @Test
    public void testDelete_WhenOrderItemDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentOrderId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().when().delete(ORDER_ITEM_ROOT_URL + "/{id}", nonExistentOrderId);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderItemService.getOrderitemNotExistErrorMsg(nonExistentOrderId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testDeleteById() {
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
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        ProductDto createdProductDto1 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        ProductDto createdProductDto2 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        OrderItemDto inputOrderItemDto1 = TestDataFactory.generateMockOrderItemDto(createdProductDto1.id(),
                createdOrderId);
        OrderItemDto createdOrderItemDto1 = given()
                .body(inputOrderItemDto1)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        OrderItemDto inputOrderItemDto2 = TestDataFactory.generateMockOrderItemDto(createdProductDto2.id(),
                createdOrderId);
        given()
                .body(inputOrderItemDto2)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        /* --- 2. Act --- */
        given()
                .delete(ORDER_ITEM_ROOT_URL + "/{id}", createdOrderItemDto1.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        OrderDto updatedOrder = given()
                .when()
                .get(ORDER_ROOT_URL + "/{id}", createdOrderId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderDto.class);

        /* --- 3. Assert --- */
        assertNotNull(updatedOrder);
        assertNotNull(updatedOrder.orderItems());
        assertFalse(updatedOrder.orderItems().isEmpty());
        assertEquals(1, updatedOrder.orderItems().size());

        var totalExpectedPrice = createdProductDto2.price()
                .multiply(BigDecimal.valueOf(inputOrderItemDto2.quantity()));

        assertEquals(totalExpectedPrice, updatedOrder.price());
    }

    // --| 3. Find OrderItem - Tests |----------------------------------------------------------------------------------

    @Test
    public void testFindAllByOrderId_WhenOrderDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentOrderId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().when().get(ORDER_ITEM_ROOT_URL + "/orders/{orderId}", nonExistentOrderId);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderService.getOrderNotExistsErrorMsg(nonExistentOrderId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindAllByOrderId() {
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
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        ProductDto createdProductDto1 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        ProductDto createdProductDto2 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        OrderItemDto inputOrderItemDto1 = TestDataFactory.generateMockOrderItemDto(createdProductDto1.id(),
                createdOrderId);
        OrderItemDto createdOrderItemDto1 = given()
                .body(inputOrderItemDto1)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        OrderItemDto inputOrderItemDto2 = TestDataFactory.generateMockOrderItemDto(createdProductDto2.id(),
                createdOrderId);
        given()
                .body(inputOrderItemDto2)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        /* --- 2. Act --- */
        Set<UUID> allOrderItemsForGivenOrder = given()
                .get(ORDER_ITEM_ROOT_URL + "/orders/{orderId}", createdOrderId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<OrderItemDto>>() {
                }).stream().map(OrderItemDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */
        assertNotNull(allOrderItemsForGivenOrder);
        assertFalse(allOrderItemsForGivenOrder.isEmpty());
        assertEquals(2, allOrderItemsForGivenOrder.size());
        assertTrue(allOrderItemsForGivenOrder.contains(createdOrderItemDto1.id()));
    }

    @Test
    public void testFindById_WhenOrderItemDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentOrderId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().when().get(ORDER_ITEM_ROOT_URL + "/{id}", nonExistentOrderId);

        /* --- 3. Assert --- */
        response.then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(OrderItemService.getOrderitemNotExistErrorMsg(nonExistentOrderId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
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
        UUID createdOrderId = UUID.fromString(given()
                .body(orderInputDto)
                .when()
                .post(ORDER_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        ProductDto createdProductDto1 = given()
                .body(TestDataFactory.generateMockProductDto()).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        OrderItemDto inputOrderItemDto1 = TestDataFactory.generateMockOrderItemDto(createdProductDto1.id(),
                createdOrderId);
        OrderItemDto createdOrderItemDto1 = given()
                .body(inputOrderItemDto1)
                .when()
                .post(ORDER_ITEM_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        /* --- 2. Act --- */
        OrderItemDto foundOrderItemDto1 = given()
                .when()
                .get(ORDER_ITEM_ROOT_URL + "/{id}", createdOrderItemDto1.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(OrderItemDto.class);

        /* --- 3. Assert --- */
        assertEquals(createdOrderItemDto1, foundOrderItemDto1);
    }
}
