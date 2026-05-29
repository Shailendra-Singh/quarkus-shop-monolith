package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

@QuarkusTest
public class CategoryServiceIntegrationTest {

    @Inject
    CategoryService categoryService;

    // --| 1. Create - Tests |------------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WhenParentCategoryDoesNotExist(UniAsserter asserter) {
        UUID fakeParentId = UUID.randomUUID();
        var inputDto = TestDataFactory.generateMockCategoryDto();
        asserter.assertFailedWith(() ->
                        categoryService.create(inputDto, fakeParentId)
                , throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("category does not exist"));
                }
        );
    }

    @Test
    @RunOnVertxContext
    public void testCreate_WhenParentCategoryIsNull(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCategoryDto();
        asserter.execute(() ->
                categoryService.create(inputDto, null).onItem().invoke(createdCategory -> {
                    assertNotNull(createdCategory);
                    assertNotNull(createdCategory.id());
                    assertNull(createdCategory.parent_category_id());
                    assertEquals(inputDto.name(), createdCategory.name());
                    assertEquals(inputDto.description(), createdCategory.description());
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testCreate_WithParentCategory(UniAsserter asserter) {
        var parentCategoryDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<UUID> parentCategoryId = new AtomicReference<>();

        asserter.execute(() ->
                categoryService
                        .create(parentCategoryDto, null)
                        .onItem()
                        .invoke(createdCategory -> parentCategoryId.set(createdCategory.id()))
        );

        var inputDto = TestDataFactory.generateMockCategoryDto();
        asserter.execute(() ->
                categoryService.create(inputDto, parentCategoryId.get()).onItem().invoke(createdCategory -> {
                    assertNotNull(createdCategory);
                    assertNotNull(createdCategory.id());
                    assertNotNull(createdCategory.parent_category_id());
                    assertEquals(parentCategoryId.get(), createdCategory.parent_category_id());
                })
        );
    }

    // --| 1. Find - Tests |--------------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindById_WhenCategoryDoesNotExist(UniAsserter asserter) {
        UUID fakeCategoryId = UUID.randomUUID();
        asserter.assertFailedWith(() ->
                        categoryService.findById(fakeCategoryId)
                , throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("category does not exist"));
                }
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<UUID> createdCategoryId = new AtomicReference<>();

        asserter.execute(() ->
                categoryService
                        .create(inputDto, null)
                        .onItem()
                        .invoke(createdCategory -> createdCategoryId.set(createdCategory.id()))
        );

        asserter.execute(() ->
                categoryService.findById(createdCategoryId.get()).invoke(result -> {
                    assertNotNull(result);
                    assertNotNull(result.id());
                    assertEquals(createdCategoryId.get(), result.id());
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {
        for (int i = 0; i < 5; i++) {
            var inputDto = TestDataFactory.generateMockCategoryDto();
            asserter.execute(() -> categoryService.create(inputDto, null));
        }

        asserter.execute(() ->
                categoryService.findAll().invoke(result -> {
                    assertNotNull(result);
                    assertFalse(result.isEmpty());
                    assertTrue(result.size() >= 5);
                })
        );
    }
}