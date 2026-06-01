package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.ProductDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.models.Product;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.ProductService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.CATEGORY_ROOT_URL;
import static me.shail.helpers.Constants.PRODUCT_ROOT_URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@QuarkusTest
@JsonTest
public class ProductResourceTest {
    // --| 1. Create Product - Tests |----------------------------------------------------------------------------------

    @Test
    public void testCreate_WithoutCategories() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();

        /* --- 2. Act --- */
        ProductDto createdProduct = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdProduct);
        assertNotNull(createdProduct.id());
        assertTrue(createdProduct.categories().isEmpty());
        assertEquals(inputProductDto.name(), createdProduct.name());
        assertEquals(inputProductDto.description(), createdProduct.description());
        assertEquals(inputProductDto.price(), createdProduct.price());
        assertEquals(inputProductDto.salesCounter(), createdProduct.salesCounter());
    }

    @Test
    public void testCreate_WithCategories_WhenCategoriesDoNotExist() {
        /* --- 1. Arrange --- */
        Set<UUID> fakeCategories = Stream
                .generate(UUID::randomUUID)
                .limit(5)
                .collect(Collectors.toUnmodifiableSet());
        var inputProductDto = TestDataFactory.generateMockProductDto(fakeCategories);

        /* --- 2. Act --- */
        var response = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.FORBIDDEN)
                .body("message", equalTo(Product.PRODUCT_ID_CATEGORY_ID_FK_CONSTRAINT_VIOLATION_ERROR_MESSAGE))
                .body("error", equalTo("Not Permitted"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate_WithCategories() {
        /* --- 1. Arrange --- */
        Set<UUID> createdCategoryIds = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
            UUID createdCategoryId = UUID.fromString(
                    given()
                            .body(inputCategoryDto).when()
                            .post(CATEGORY_ROOT_URL)
                            .then()
                            .statusCode(RestResponse.StatusCode.OK)
                            .extract()
                            .path("id"));
            createdCategoryIds.add(createdCategoryId);
        }
        ProductDto inputProductDto = TestDataFactory.generateMockProductDto(createdCategoryIds);

        /* --- 2. Act --- */
        ProductDto createdProduct = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdProduct);
        assertNotNull(createdProduct.id());
        assertFalse(createdProduct.categories().isEmpty());
        assertEquals(createdCategoryIds, createdProduct.categories());
        assertEquals(inputProductDto.name(), createdProduct.name());
        assertEquals(inputProductDto.description(), createdProduct.description());
        assertEquals(inputProductDto.price(), createdProduct.price());
        assertEquals(inputProductDto.salesCounter(), createdProduct.salesCounter());
    }

    // --| 2. Update Product - Tests |----------------------------------------------------------------------------------

    @Test
    public void testAddCategoryToProduct_WhenProductOrCategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID fakeProductId = UUID.randomUUID();
        UUID fakeCategoryId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().when()
                .put(PRODUCT_ROOT_URL + "/{id}/categories/{categoryId}", fakeProductId, fakeCategoryId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.FORBIDDEN)
                .body("message", equalTo(String.format("FK Constraint Violation. " +
                        "Invalid Product (Id: %s) or Category (Id: %s)", fakeProductId, fakeCategoryId)))
                .body("error", equalTo("Not Permitted"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testAddCategoryToProduct() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdCategoryId = UUID.fromString(
                given()
                        .body(inputCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        /* --- 2. Act --- */
        given().when()
                .put(PRODUCT_ROOT_URL + "/{id}/categories/{categoryId}", createdProductId, createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
        ProductDto updatedProduct = given()
                .get(PRODUCT_ROOT_URL + "/{id}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        assertNotNull(updatedProduct);
        assertNotNull(updatedProduct.categories());
        assertFalse(updatedProduct.categories().isEmpty());
        assertTrue(updatedProduct.categories().contains(createdCategoryId));
    }

    @Test
    public void testRemoveCategoryFromProduct_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID fakeProductId = UUID.randomUUID();
        UUID fakeCategoryId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .delete(PRODUCT_ROOT_URL + "/{id}/categories/{categoryId}", fakeProductId, fakeCategoryId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(fakeProductId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testRemoveCategoryFromProduct_CategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));
        UUID fakeCategoryId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .delete(PRODUCT_ROOT_URL + "/{id}/categories/{categoryId}", createdProductId, fakeCategoryId);

        /* --- 3. Assert --- */
        // Idempotent call.
        response.then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);
    }

    @Test
    public void testRemoveCategoryFromProduct() {
        /* --- 1. Arrange --- */
        Set<UUID> createdCategoryIds = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
            UUID createdCategoryId = UUID.fromString(
                    given()
                            .body(inputCategoryDto).when()
                            .post(CATEGORY_ROOT_URL)
                            .then()
                            .statusCode(RestResponse.StatusCode.OK)
                            .extract()
                            .path("id"));
            createdCategoryIds.add(createdCategoryId);
        }

        var inputProductDto = TestDataFactory.generateMockProductDto(createdCategoryIds);
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        UUID categoryToBeRemoved = createdCategoryIds.stream().findAny().orElse(null);

        /* --- 2. Act --- */
        given().when()
                .delete(PRODUCT_ROOT_URL + "/{id}/categories/{categoryId}", createdProductId, categoryToBeRemoved)
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
        ProductDto updatedProduct = given()
                .get(PRODUCT_ROOT_URL + "/{id}", createdProductId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        assertNotNull(updatedProduct);
        assertFalse(updatedProduct.categories().isEmpty());
        assertEquals(createdCategoryIds.size() - 1, updatedProduct.categories().size());
    }

    // --| 3. Delete Product - Tests |----------------------------------------------------------------------------------

    @Test
    public void testDeleteById_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given().delete(PRODUCT_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        // Idempotent call.
        response
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);
    }

    @Test
    public void testDeleteById() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        ProductDto createdProduct = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 2. Act --- */
        given().delete(PRODUCT_ROOT_URL + "/{id}", createdProduct.id())
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        /* --- 3. Assert --- */
        given()
                .head(PRODUCT_ROOT_URL + "/{id}", createdProduct.id())
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND);
    }

    // --| 4. Find Product - Tests |------------------------------------------------------------------------------------

    @Test
    public void testFindById_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID fakeProductId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .get(PRODUCT_ROOT_URL + "/{id}", fakeProductId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(ProductService.getProductNotExistErrorMsg(fakeProductId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        ProductDto createdProduct = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 2. Act --- */
        ProductDto foundProduct = given()
                .get(PRODUCT_ROOT_URL + "/{id}", createdProduct.id())
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 3. Assert --- */
        assertEquals(createdProduct, foundProduct);
    }

    @Test
    public void testExistById_WhenProductDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID fakeProductId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .head(PRODUCT_ROOT_URL + "/{id}", fakeProductId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND);
    }

    @Test
    public void testExistById() {
        /* --- 1. Arrange --- */
        var inputProductDto = TestDataFactory.generateMockProductDto();
        ProductDto createdProduct = given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(ProductDto.class);

        /* --- 2. Act --- */
        var response = given()
                .head(PRODUCT_ROOT_URL + "/{id}", createdProduct.id());

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.OK);
    }

    @Test
    public void testFindByCategoryId() {
        /* --- 1. Arrange --- */
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdCategoryId = UUID.fromString(
                given()
                        .body(inputCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        var inputProductDto = TestDataFactory.generateMockProductDto(Set.of(createdCategoryId));
        UUID createdProductId = UUID.fromString(given()
                .body(inputProductDto).when()
                .post(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .path("id"));

        /* --- 2. Act --- */
        Set<UUID> allProductsForGivenCategory = given()
                .get(PRODUCT_ROOT_URL + "/categories/{categoryId}", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<ProductDto>>() {
                }).stream().map(ProductDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */
        assertTrue(allProductsForGivenCategory.contains(createdProductId));
    }

    @Test
    public void testFindAll() {
        /* --- 1. Arrange --- */
        Set<UUID> createdProductIds = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            var inputProductDto = TestDataFactory.generateMockProductDto();
            UUID createdProductId = UUID.fromString(
                    given()
                            .body(inputProductDto).when()
                            .post(PRODUCT_ROOT_URL)
                            .then()
                            .statusCode(RestResponse.StatusCode.OK)
                            .extract()
                            .path("id"));
            createdProductIds.add(createdProductId);
        }

        /* --- 2. Act --- */
        Set<UUID> allProductIds = given()
                .get(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<ProductDto>>() {
                }).stream().map(ProductDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */
        assertTrue(allProductIds.containsAll(createdProductIds));
    }

    // --| 5. Count Product - Tests |-----------------------------------------------------------------------------------

    @Test
    public void testCountAll() {
        /* --- 1. Arrange --- */
        for (int i = 0; i < 5; i++) {
            var inputProductDto = TestDataFactory.generateMockProductDto();
            given()
                    .body(inputProductDto).when()
                    .post(PRODUCT_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id");
        }

        Set<UUID> allProductIds = given()
                .get(PRODUCT_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<ProductDto>>() {
                }).stream().map(ProductDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 2. Act --- */
        long productsCount = given()
                .get(PRODUCT_ROOT_URL + "/counts")
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        /* --- 3. Assert --- */
        assertEquals(allProductIds.size(), productsCount);
    }

    @Test
    public void testCountByCategoryId() {
        /* --- 1. Arrange --- */
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdCategoryId = UUID.fromString(
                given()
                        .body(inputCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        for (int i = 0; i < 2; i++) {
            var inputProductDto = TestDataFactory.generateMockProductDto(Set.of(createdCategoryId));
            given()
                    .body(inputProductDto).when()
                    .post(PRODUCT_ROOT_URL)
                    .then()
                    .statusCode(RestResponse.StatusCode.OK)
                    .extract()
                    .path("id");
        }

        /* --- 2. Act --- */
        long productsCount = given()
                .get(PRODUCT_ROOT_URL + "/counts/categories/{categoryId}", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        /* --- 3. Assert --- */
        assertEquals(2, productsCount);
    }
}
