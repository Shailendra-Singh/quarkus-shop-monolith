package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.common.BaseTest;
import me.shail.dtos.ProductDto;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@QuarkusTest
public class ProductServiceIntegrationTest extends BaseTest {
    @Inject
    ProductService productService;

    @Inject
    CategoryService categoryService;

    // --| 1. Create Product - Tests |----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreate_WithoutCategories(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        asserter.execute(() -> productService.create(inputDto).invoke(result -> {
            assertNotNull(result);
            assertNotNull(result.id());
            assertTrue(result.categories().isEmpty());
            assertEquals(inputDto.name(), result.name());
            assertEquals(inputDto.description(), result.description());
            assertEquals(inputDto.price(), result.price());
            assertEquals(inputDto.salesCounter(), result.salesCounter());
        }));
    }

    @Test
    @RunOnVertxContext
    public void testCreate_WithCategories_WhenCategoriesDoNotExist(UniAsserter asserter) {
        Set<UUID> fakeCategories = Stream
                .generate(UUID::randomUUID)
                .limit(5)
                .collect(Collectors.toUnmodifiableSet());
        var inputDto = TestDataFactory.generateMockProductDto(fakeCategories);

        asserter.assertFailedWith(() -> productService.create(inputDto), throwable -> {
            assertNotNull(throwable);
            assertEquals(OperationNotSupportedException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("fk constraint violation"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testCreate_WithCategories(UniAsserter asserter) {
        AtomicReference<Set<UUID>> createdCategoriesUUIDs = new AtomicReference<>(new HashSet<>());
        for (int i = 0; i < 5; i++) {
            var inputDto = TestDataFactory.generateMockCategoryDto();
            asserter.execute(() -> categoryService.create(inputDto, null)
                    .invoke(createdCategory -> createdCategoriesUUIDs
                            .get()
                            .add(createdCategory.id())
                    )
            );
        }
        var inputDto = TestDataFactory.generateMockProductDto(createdCategoriesUUIDs.get());

        asserter.execute(() -> productService.create(inputDto).invoke(result -> {
            assertNotNull(result);
            assertNotNull(result.id());
            assertFalse(result.categories().isEmpty());
            assertEquals(createdCategoriesUUIDs.get().size(), result.categories().size());
            assertEquals(inputDto.name(), result.name());
            assertEquals(inputDto.description(), result.description());
            assertEquals(inputDto.price(), result.price());
            assertEquals(inputDto.salesCounter(), result.salesCounter());
        }));
    }

    // --| 2. Update Product - Tests |----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testAddCategoryToProduct_WhenProductOrCategoryDoesNotExist(UniAsserter asserter) {
        UUID fakeProductId = UUID.randomUUID();
        UUID fakeCategoryId = UUID.randomUUID();
        asserter.assertFailedWith(
                () -> productService.addCategoryToProduct(fakeProductId, fakeCategoryId),
                throwable -> {
                    assertNotNull(throwable);
                    assertEquals(OperationNotSupportedException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("fk constraint violation"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testAddCategoryToProduct(UniAsserter asserter) {
        var inputProductDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputProductDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<UUID> createdCategoryId = new AtomicReference<>();

        asserter.execute(() ->
                categoryService.create(inputCategoryDto, null)
                        .onItem()
                        .invoke(createdCategory -> createdCategoryId.set(createdCategory.id())
                        )
        );

        asserter.execute(() -> productService
                .addCategoryToProduct(createdProductId.get(), createdCategoryId.get())
                .invoke(Assertions::assertTrue)
        );

        asserter.execute(() -> productService.findById(createdProductId.get())
                .invoke(returnedProduct -> {
                    assertNotNull(returnedProduct);
                    assertNotNull(returnedProduct.categories());
                    assertFalse(returnedProduct.categories().isEmpty());
                    assertTrue(returnedProduct.categories().contains(createdCategoryId.get()));
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testRemoveCategoryFromProduct_WhenProductDoesNotExist(UniAsserter asserter) {
        UUID fakeProductId = UUID.randomUUID();
        UUID fakeCategoryId = UUID.randomUUID();
        asserter.assertFailedWith(() -> productService
                        .removeCategoryFromProduct(fakeProductId, fakeCategoryId),
                throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("product does not exist"));
                }
        );
    }

    @Test
    @RunOnVertxContext
    public void testRemoveCategoryFromProduct_CategoryDoesNotExist(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(createdProduct -> createdProductId.set(createdProduct.id()))
        );

        UUID fakeCategoryId = UUID.randomUUID();
        asserter.execute(() -> productService
                .removeCategoryFromProduct(createdProductId.get(), fakeCategoryId)
                .invoke(Assertions::assertFalse)
        );
    }

    @Test
    @RunOnVertxContext
    public void testRemoveCategoryFromProduct(UniAsserter asserter) {
        AtomicReference<Set<UUID>> createdCategoriesUUIDs = new AtomicReference<>(new HashSet<>());
        for (int i = 0; i < 5; i++) {
            var inputDto = TestDataFactory.generateMockCategoryDto();
            asserter.execute(() -> categoryService.create(inputDto, null)
                    .invoke(createdCategory -> createdCategoriesUUIDs
                            .get()
                            .add(createdCategory.id())
                    )
            );
        }
        var inputDto = TestDataFactory.generateMockProductDto(createdCategoriesUUIDs.get());

        AtomicReference<UUID> createdProductId = new AtomicReference<>();
        asserter.execute(() -> productService.create(inputDto)
                .invoke(createdProduct -> createdProductId.set(createdProduct.id()))
        );

        asserter.execute(() -> productService
                .removeCategoryFromProduct(createdProductId.get(),
                        createdCategoriesUUIDs.get().stream().findAny().orElse(null)
                ).invoke(Assertions::assertTrue)
        );

        asserter.execute(() -> productService.findById(createdProductId.get()).invoke(returnedProduct -> {
                    assertNotNull(returnedProduct);
                    assertFalse(returnedProduct.categories().isEmpty());
                    assertEquals(createdCategoriesUUIDs.get().size() - 1, returnedProduct.categories().size());
                })
        );
    }

    // --| 3. Delete Product - Tests |----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testDeleteById_WhenProductDoesNotExist(UniAsserter asserter) {
        UUID fakeProductId = UUID.randomUUID();

        asserter.execute(() -> productService.deleteById(fakeProductId)
                .invoke(Assertions::assertFalse));
    }

    @Test
    @RunOnVertxContext
    public void testDeleteById(UniAsserter asserter) {
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<Set<UUID>> createdCategoriesUUIDs = new AtomicReference<>(new HashSet<>());
        asserter.execute(() -> categoryService.create(inputCategoryDto, null)
                .invoke(createdCategory -> createdCategoriesUUIDs.get().add(createdCategory.id())
                )
        );

        var inputProductDto = TestDataFactory.generateMockProductDto(createdCategoriesUUIDs.get());
        AtomicReference<UUID> toBeDeletedProductId = new AtomicReference<>();

        asserter.execute(() -> productService
                .create(inputProductDto)
                .invoke(createdProductDto -> toBeDeletedProductId.set(createdProductDto.id()))
        );

        asserter.execute(() -> productService
                .create(TestDataFactory.generateMockProductDto(createdCategoriesUUIDs.get()))
        );

        asserter.execute(() -> productService.deleteById(toBeDeletedProductId.get())
                .invoke(Assertions::assertTrue));

        //noinspection OptionalGetWithoutIsPresent
        asserter.execute(() -> productService
                .countByCategoryId(createdCategoriesUUIDs
                        .get()
                        .stream().findFirst()
                        .get())
                .invoke(count -> assertEquals(1, count))
        );
    }

    // --| 4. Find Product - Tests |------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindById_WhenProductDoesNotExist(UniAsserter asserter) {
        UUID fakeProductId = UUID.randomUUID();

        asserter.assertFailedWith(() -> productService.findById(fakeProductId), throwable -> {
            assertEquals(EntityNotFoundException.class, throwable.getClass());
            assertTrue(throwable.getMessage().toLowerCase().contains("product does not exist"));
        });
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        AtomicReference<Set<UUID>> createdCategoriesUUIDs = new AtomicReference<>(new HashSet<>());
        for (int i = 0; i < 5; i++) {
            var inputDto = TestDataFactory.generateMockCategoryDto();
            asserter.execute(() -> categoryService.create(inputDto, null)
                    .invoke(createdCategory -> createdCategoriesUUIDs
                            .get()
                            .add(createdCategory.id())
                    )
            );
        }
        var inputDto = TestDataFactory.generateMockProductDto(createdCategoriesUUIDs.get());

        AtomicReference<ProductDto> createdProductRef = new AtomicReference<>();
        asserter.execute(() -> productService.create(inputDto).invoke(createdProductRef::set));

        asserter.execute(() ->
                productService.findById(createdProductRef.get().id()).invoke(returnedProduct -> {
                    assertNotNull(returnedProduct);
                    assertNotNull(returnedProduct.id());
                    assertEquals(createdProductRef.get(), returnedProduct);
                    assertEquals(inputDto.categories().size(), returnedProduct.categories().size());
                    assertTrue(returnedProduct.categories().containsAll(inputDto.categories()));
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testExistById_WhenProductDoesNotExist(UniAsserter asserter) {
        UUID fakeProductId = UUID.randomUUID();
        asserter.execute(() -> productService.existById(fakeProductId).invoke(Assertions::assertFalse));
    }

    @Test
    @RunOnVertxContext
    public void testExistById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto).invoke(p -> createdProductId.set(p.id())));
        asserter.execute(() -> productService.existById(createdProductId.get()).invoke(Assertions::assertTrue));
    }

    @Test
    @RunOnVertxContext
    public void testFindByCategoryId(UniAsserter asserter) {
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<UUID> createdCategoryId = new AtomicReference<>();
        asserter.execute(() -> categoryService.create(inputCategoryDto, null)
                .invoke(createdCategory -> createdCategoryId.set(createdCategory.id())
                )
        );

        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto(createdCategoryId.get())));
        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto(createdCategoryId.get())));

        asserter.execute(() -> productService
                .findByCategoryId(createdCategoryId.get())
                .invoke(returnedProducts -> {
                    assertNotNull(returnedProducts);
                    assertFalse(returnedProducts.isEmpty());
                    assertEquals(2, returnedProducts.size());
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindAll(UniAsserter asserter) {

        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto()));
        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto()));

        asserter.execute(() -> productService
                .findAll()
                .invoke(returnedProducts -> {
                    assertNotNull(returnedProducts);
                    assertFalse(returnedProducts.isEmpty());
                    assertEquals(2, returnedProducts.size());
                })
        );
    }

    // --| 5. Count Product - Tests |-----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCountAll(UniAsserter asserter) {

        AtomicReference<Long> findAllCounts = new AtomicReference<>();

        asserter.execute(() -> productService.findAll()
                .invoke(productDtos -> findAllCounts.set((long) productDtos.size()))
        );

        asserter.execute(() -> productService
                .countAll()
                .invoke(count -> assertEquals(findAllCounts.get(), count))
        );
    }

    @Test
    @RunOnVertxContext
    public void testCountByCategoryId(UniAsserter asserter) {
        var inputCategoryDto = TestDataFactory.generateMockCategoryDto();
        AtomicReference<UUID> createdCategoryId = new AtomicReference<>();
        asserter.execute(() -> categoryService.create(inputCategoryDto, null)
                .invoke(createdCategory -> createdCategoryId.set(createdCategory.id())
                )
        );

        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto(createdCategoryId.get())));
        asserter.execute(() -> productService.create(TestDataFactory.generateMockProductDto(createdCategoryId.get())));

        asserter.execute(() -> productService
                .countByCategoryId(createdCategoryId.get())
                .invoke(count -> assertEquals(2, count))
        );
    }
}