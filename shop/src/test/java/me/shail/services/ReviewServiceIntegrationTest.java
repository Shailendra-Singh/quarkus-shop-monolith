package me.shail.services;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import me.shail.helpers.data.TestDataFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ReviewServiceIntegrationTest {
    @Inject
    ReviewService reviewService;

    @Inject
    ProductService productService;

    // --| 1. Create Review - Tests |-----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testCreateCart_WhenProductDoesNotExist(UniAsserter asserter) {
        var nonExistentCustomer = UUID.randomUUID();
        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        asserter.assertFailedWith(
                () -> reviewService.create(reviewInputDto, nonExistentCustomer), throwable -> {
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("product does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testCreateCart(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        asserter.execute(
                () -> reviewService.create(reviewInputDto, createdProductId.get())
                        .invoke(createdReview -> {
                            assertNotNull(createdReview);
                            assertNotNull(createdReview.id());
                            assertEquals(createdReview.title(), reviewInputDto.title());
                            assertEquals(createdReview.description(), reviewInputDto.description());
                            assertEquals(createdReview.rating(), reviewInputDto.rating());
                        })
        );
    }

    // --| 2. Find Review - Tests |-------------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testFindById_WhenReviewDoesNotExist(UniAsserter asserter) {
        var fakeReviewId = UUID.randomUUID();

        asserter.assertFailedWith(
                () -> reviewService.findById(fakeReviewId), throwable -> {
                    assertNotNull(throwable);
                    assertEquals(EntityNotFoundException.class, throwable.getClass());
                    assertTrue(throwable.getMessage().toLowerCase().contains("review does not exist"));
                });
    }

    @Test
    @RunOnVertxContext
    public void testFindById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        AtomicReference<UUID> createdReviewId = new AtomicReference<>();

        asserter.execute(
                () -> reviewService.create(reviewInputDto, createdProductId.get())
                        .invoke(createdReview -> createdReviewId.set(createdReview.id()))
        );

        asserter.execute(() -> reviewService.findById(createdReviewId.get())
                .invoke(foundReview -> {
                    assertNotNull(foundReview);
                    assertNotNull(foundReview.id());
                    assertEquals(createdReviewId.get(), foundReview.id());
                    assertEquals(reviewInputDto.title(), foundReview.title());
                    assertEquals(reviewInputDto.description(), foundReview.description());
                    assertEquals(reviewInputDto.rating(), foundReview.rating());
                })
        );
    }

    @Test
    @RunOnVertxContext
    public void testFindReviewsByProductId_WhenProductDoesNotExist(UniAsserter asserter) {
        var fakeProductId = UUID.randomUUID();

        asserter.execute(
                () -> reviewService.findReviewsByProductId(fakeProductId).invoke(result -> {
                    assertNotNull(result);
                    assertTrue(result.isEmpty());
                }));
    }

    @Test
    @RunOnVertxContext
    public void testFindAndCountReviewsByProductId(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        for (var reviewInputDto : TestDataFactory.generateMockReviewDtos(5))
            asserter.execute(() -> reviewService.create(reviewInputDto, createdProductId.get()));


        asserter.execute(
                () -> reviewService.findReviewsByProductId(createdProductId.get())
                        .invoke(foundReviews -> {
                            assertNotNull(foundReviews);
                            assertFalse(foundReviews.isEmpty());
                            assertEquals(5, foundReviews.size());
                        })
        );

        asserter.execute(() ->
                reviewService.countReviewsByProductId(createdProductId.get())
                        .invoke(reviewCount -> assertEquals(5, reviewCount))
        );
    }

    @Test
    @RunOnVertxContext
    public void testCountReviewsByProductId_WhenProductDoesNotExist(UniAsserter asserter) {
        var fakeProductId = UUID.randomUUID();

        asserter.execute(
                () -> reviewService.countReviewsByProductId(fakeProductId)
                        .invoke(reviewCount -> assertEquals(0, reviewCount))
        );
    }

    // --| 3. Delete Review - Tests |-----------------------------------------------------------------------------------

    @Test
    @RunOnVertxContext
    public void testDeleteById_WhenReviewDoesNotExist(UniAsserter asserter) {
        var fakeReviewId = UUID.randomUUID();
        asserter.execute(() -> reviewService.deleteById(fakeReviewId).invoke(Assertions::assertFalse));
    }

    @Test
    @RunOnVertxContext
    public void testDeleteById(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        var reviewInputDto = TestDataFactory.generateMockReviewDto();

        AtomicReference<UUID> createdReviewId = new AtomicReference<>();

        asserter.execute(
                () -> reviewService.create(reviewInputDto, createdProductId.get())
                        .invoke(createdReview -> createdReviewId.set(createdReview.id()))
        );

        asserter.execute(() -> reviewService.deleteById(createdReviewId.get()).invoke(Assertions::assertTrue));
    }

    @Test
    @RunOnVertxContext
    public void testDeleteAllReviewsByProductId_WhenProductDoesNotExist(UniAsserter asserter) {
        var fakeProductId = UUID.randomUUID();

        asserter.execute(
                () -> reviewService.deleteAllReviewsByProductId(fakeProductId)
                        .invoke(reviewCount -> assertEquals(0, reviewCount))
        );
    }

    @Test
    @RunOnVertxContext
    public void testDeleteAllReviewsByProductId(UniAsserter asserter) {
        var inputDto = TestDataFactory.generateMockProductDto();
        AtomicReference<UUID> createdProductId = new AtomicReference<>();

        asserter.execute(() -> productService.create(inputDto)
                .invoke(result -> createdProductId.set(result.id()))
        );

        for (var reviewInputDto : TestDataFactory.generateMockReviewDtos(5))
            asserter.execute(() -> reviewService.create(reviewInputDto, createdProductId.get()));


        asserter.execute(
                () -> reviewService.deleteAllReviewsByProductId(createdProductId.get())
                        .invoke(deletedReviewCount -> assertEquals(5, deletedReviewCount))
        );

        asserter.execute(() ->
                reviewService.countReviewsByProductId(createdProductId.get())
                        .invoke(reviewCount -> assertEquals(0, reviewCount))
        );
    }
}