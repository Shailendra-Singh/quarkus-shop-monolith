package me.shail.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import me.shail.dtos.CategoryDto;
import me.shail.helpers.data.TestDataFactory;
import me.shail.resources.interceptors.JsonTest;
import me.shail.services.CategoryService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static me.shail.helpers.Constants.CATEGORY_ROOT_URL;
import static me.shail.helpers.Constants.PRODUCT_ROOT_URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@QuarkusTest
@JsonTest
public class CategoryResourceTest {

    // --| 1. Create - Tests |------------------------------------------------------------------------------------------

    @Test
    public void testCreate_WhenParentCategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();

        /* --- 2. Act --- */
        var response = given()
                .queryParam("parentCategoryId", nonExistentId)
                .body(inputCategoryDto)
                .post(CATEGORY_ROOT_URL);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CategoryService.getCategoryNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testCreate_WhenParentCategoryIsNull() {
        /* --- 1. Arrange --- */
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();

        /* --- 2. Act --- */
        CategoryDto createdCategory = given()
                .body(inputCategoryDto).when()
                .post(CATEGORY_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CategoryDto.class);

        /* --- 3. Assert --- */
        assertNotNull(createdCategory);
        assertNotNull(createdCategory.id());
        assertNull(createdCategory.parent_category_id());
        assertEquals(inputCategoryDto.name(), createdCategory.name());
        assertEquals(inputCategoryDto.description(), createdCategory.description());
    }

    @Test
    public void testCreate_WithParentCategory() {
        /* --- 1. Arrange --- */
        var inputParentCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdParentCategoryId = UUID.fromString(
                given()
                        .body(inputParentCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        /* --- 2. Act --- */
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        CategoryDto createdCategory = given()
                .queryParam("parentCategoryId", createdParentCategoryId)
                .body(inputCategoryDto).when()
                .post(CATEGORY_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CategoryDto.class);

        assertNotNull(createdCategory);
        assertNotNull(createdCategory.id());
        assertNotNull(createdCategory.parent_category_id());
        assertEquals(createdParentCategoryId, createdCategory.parent_category_id());
    }

    // --| 2. Delete - Tests |------------------------------------------------------------------------------------------

    @Test
    public void testRemoveAllProductsFromCategory_WhenCategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .delete(CATEGORY_ROOT_URL + "/{id}/products", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CategoryService.getCategoryNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testRemoveAllProductsFromCategory() {
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
        long previousProductCounts = given()
                .get(PRODUCT_ROOT_URL + "/counts/categories/{categoryId}", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        given()
                .when()
                .delete(CATEGORY_ROOT_URL + "/{id}/products", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);

        long currentProductCounts = given()
                .get(PRODUCT_ROOT_URL + "/counts/categories/{categoryId}", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(Long.class);

        /* --- 3. Assert --- */
        // Check counts
        assertEquals(2L, previousProductCounts);
        assertEquals(0L, currentProductCounts);
        // Ensure category exists
        given()
                .head(CATEGORY_ROOT_URL + "/{id}", createdCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK);
    }

    // --| 3. Find - Tests |--------------------------------------------------------------------------------------------

    @Test
    public void testExistById_WhenCategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .head(CATEGORY_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND);
    }

    @Test
    public void testExistById() {
        /* --- 1. Arrange --- */
        var inputParentCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdParentCategoryId = UUID.fromString(
                given()
                        .body(inputParentCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        /* --- 2. Act --- */
        var response = given()
                .head(CATEGORY_ROOT_URL + "/{id}", createdParentCategoryId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.OK);
    }

    @Test
    public void testFindById_WhenCategoryDoesNotExist() {
        /* --- 1. Arrange --- */
        UUID nonExistentId = UUID.randomUUID();

        /* --- 2. Act --- */
        var response = given()
                .get(CATEGORY_ROOT_URL + "/{id}", nonExistentId);

        /* --- 3. Assert --- */
        response
                .then()
                .statusCode(RestResponse.StatusCode.NOT_FOUND)
                .body("message", equalTo(CategoryService.getCategoryNotExistErrorMsg(nonExistentId)))
                .body("error", equalTo("Not Found"))
                .body("timestamp", notNullValue());
    }

    @Test
    public void testFindById() {
        /* --- 1. Arrange --- */
        var inputParentCategoryDto = TestDataFactory.generateMockCategoryDto();
        UUID createdParentCategoryId = UUID.fromString(
                given()
                        .body(inputParentCategoryDto).when()
                        .post(CATEGORY_ROOT_URL)
                        .then()
                        .statusCode(RestResponse.StatusCode.OK)
                        .extract()
                        .path("id"));

        /* --- 2. Act --- */
        CategoryDto foundCategory = given()
                .get(CATEGORY_ROOT_URL + "/{id}", createdParentCategoryId)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(CategoryDto.class);

        /* --- 3. Assert --- */
        assertNotNull(foundCategory);
        assertNotNull(foundCategory.id());
        assertEquals(createdParentCategoryId, foundCategory.id());
    }

    @Test
    public void testFindAll() {
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

        /* --- 2. Act --- */
        Set<UUID> allCategoryIds = given()
                .get(CATEGORY_ROOT_URL)
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body()
                .as(new TypeRef<List<CategoryDto>>() {
                }).stream().map(CategoryDto::id).collect(Collectors.toUnmodifiableSet());

        /* --- 3. Assert --- */
        assertTrue(allCategoryIds.containsAll(createdCategoryIds));
    }
}
