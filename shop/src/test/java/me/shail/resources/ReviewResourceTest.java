package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import me.shail.dtos.ReviewDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.ProductService;
import me.shail.services.ReviewService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.PRODUCT_ROOT_URL;
import static me.shail.helpers.Constants.REVIEW_ROOT_URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@JsonTest
public class ReviewResourceTest {

    // --| 1. Create Review - Tests |-----------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();
        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        /* --- 2. Act --- */
        var response = given()
                .body(reviewInputDto)
                .when()
                .post(REVIEW_ROOT_URL + "/products/{productId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        /* --- 2. Act --- */
        ReviewDto createdReview = given()
                .body(reviewInputDto)
                .when()
                .post(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ReviewDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdReview);
        assertNotNull(createdReview.id());
        assertEquals(createdReview.title(), reviewInputDto.title());
        assertEquals(createdReview.description(), reviewInputDto.description());
        assertEquals(createdReview.rating(), reviewInputDto.rating());
    }

    // --| 2. Find Review - Tests |-------------------------------------------------------------------------------------

    @Test
    public void testFindById_WhenReviewDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .when()
                .get(REVIEW_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ReviewService.getReviewNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        ReviewDto createdReview = given()
                .body(reviewInputDto)
                .when()
                .post(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ReviewDto.class);

        /* --- 2. Act --- */
        ReviewDto foundReview = given()
                .when()
                .get(REVIEW_ROOT_URL + "/{id}", createdReview.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ReviewDto.class);

        /* --- 3. Assert --- */
        assertEquals(createdReview, foundReview);
    }

    @Test
    public void testFindReviewsByProductId_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .get(REVIEW_ROOT_URL + "/products/{productId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindAndCountReviewsByProductId() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        Set<UUID> createdReviewIds = new HashSet<>();

        for (var reviewInputDto : TestDataFactory.generateMockReviewDtos(5)) {
            UUID createdReviewId = UUID.fromString(given()
                    .body(reviewInputDto)
                    .when()
                    .post(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id"));
            createdReviewIds.add(createdReviewId);
        }

        /* --- 2. Act --- */
        long reviewsCount = given()
                .get(REVIEW_ROOT_URL + "/products/{productId}/count", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        /* --- 3. Assert --- */
        assertEquals(createdReviewIds.size(), reviewsCount);
    }

    @Test
    public void testCountReviewsByProductId_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .get(REVIEW_ROOT_URL + "/products/{productId}/count", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    // --| 3. Delete Review - Tests |-----------------------------------------------------------------------------------

    @Test
    public void testDeleteById_WhenReviewDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .when()
                .delete(REVIEW_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ReviewService.getReviewNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testDeleteById() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        ReviewDto createdReview = given()
                .body(reviewInputDto)
                .when()
                .post(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ReviewDto.class);

        /* --- 2. Act --- */
        given()
                .when()
                .delete(REVIEW_ROOT_URL + "/{id}", createdReview.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
         given()
                .when()
                .get(REVIEW_ROOT_URL + "/{id}", createdReview.id())
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ReviewService.getReviewNotExistErrorMsg(createdReview.id())))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testDeleteAllReviewsByProductId_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .delete(REVIEW_ROOT_URL + "/products/{productId}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testDeleteAllReviewsByProductId() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        for (var reviewInputDto : TestDataFactory.generateMockReviewDtos(5)) {
            given()
                    .body(reviewInputDto)
                    .when()
                    .post(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK);
        }

        /* --- 2. Act --- */
        given()
                .delete(REVIEW_ROOT_URL + "/products/{productId}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
        long reviewsCount = given()
                .get(REVIEW_ROOT_URL + "/products/{productId}/count", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        assertEquals(0, reviewsCount);
    }
}
