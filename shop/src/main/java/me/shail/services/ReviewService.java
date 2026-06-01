package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.ReviewDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Product;
import me.shail.models.Review;
import me.shail.repositories.ProductRepository;
import me.shail.repositories.ReviewRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class ReviewService {

    public final static String REVIEW_NOT_EXIST_ERROR_MSG = "Review does not exist. Id: ";

    @Inject
    ReviewRepository reviewRepository;

    @Inject
    ProductRepository productRepository;

    public static ReviewDto mapToDto(Review review) {
        return new ReviewDto(
                review.id,
                review.title,
                review.description,
                review.rating
        );
    }

    @WithCustomStatelessSession
    public Uni<Long> countReviewsByProductId(UUID productId) {
        log.debug("Request to get count of Reviews for Product Id: {}", productId);
        return ProductService.generateUni_ExistById(this.productRepository, productId, false)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException(ProductService.getProductNotExistErrorMsg(productId))
                        );
                    return this.reviewRepository.countReviewsByProductId(productId);
                });
    }

    @WithCustomStatelessSession
    public Uni<List<ReviewDto>> findReviewsByProductId(UUID productId) {
        log.debug("Request to get all Reviews");
        return ProductService.generateUni_ExistById(this.productRepository, productId, false)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException(ProductService.getProductNotExistErrorMsg(productId))
                        );
                    return this.reviewRepository.findReviewsByProductId(productId)
                            .map(reviews -> reviews.stream()
                                    .map(ReviewService::mapToDto)
                                    .toList());
                });
    }

    @WithCustomStatelessSession
    public Uni<ReviewDto> findById(UUID reviewId) {
        log.debug("Request to get Review: {}", reviewId);
        return generateUni_FindById(this.reviewRepository, reviewId, false)
                .map(ReviewService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<Boolean> existById(UUID reviewId) {
        log.debug("Request to check if exists Review: {}", reviewId);
        return generateUni_ExistById(this.reviewRepository, reviewId, false);
    }

    @WithTransaction
    public Uni<ReviewDto> create(ReviewDto reviewDto, UUID productId) {
        log.debug("Request to create Review: {} for Product: {}", reviewDto, productId);

        return ProductService
                .generateUni_ExistById(this.productRepository, productId, true)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException(ProductService.getProductNotExistErrorMsg(productId))
                        );

                    return this.reviewRepository
                            .getSession().map(session -> session.getReference(Product.class, productId))
                            .chain(productProxy -> {
                                Review savedReview = new Review(reviewDto.title(),
                                        reviewDto.description(),
                                        reviewDto.rating(),
                                        productProxy);
                                return this.reviewRepository.create(savedReview);
                            });
                }).map(ReviewService::mapToDto);
    }

    @WithTransaction
    public Uni<Boolean> deleteById(UUID reviewId) {
        log.debug("Request to delete Review: {}", reviewId);
        return generateUni_ExistById(this.reviewRepository, reviewId, true)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(()
                                -> new EntityNotFoundException(getReviewNotExistErrorMsg(reviewId))
                        );

                    return this.reviewRepository.deleteById(reviewId);
                });
    }

    @WithTransaction
    public Uni<Integer> deleteAllReviewsByProductId(UUID productId) {
        log.debug("Request to delete all reviews for Product Id: {}", productId);
        return ProductService.generateUni_ExistById(this.productRepository, productId, true)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException(ProductService.getProductNotExistErrorMsg(productId))
                        );
                    return this.reviewRepository.deleteReviewsByProductId(productId);
                });
    }

    public static Uni<Review> generateUni_FindById(ReviewRepository repository, UUID reviewId, boolean managed) {
        Uni<Review> generatedUni = managed
                ? repository.findByIdManaged(reviewId)
                : repository.findByIdStateless(reviewId);

        return generatedUni.onItem().ifNull().failWith(() ->
                new EntityNotFoundException(getReviewNotExistErrorMsg(reviewId))
        );
    }

    public static Uni<Boolean> generateUni_ExistById(ReviewRepository repository, UUID reviewId, boolean managed) {
        return managed
                ? repository.existByIdManaged(reviewId)
                : repository.existByIdStateless(reviewId);
    }

    public static String getReviewNotExistErrorMsg(UUID reviewId) {
        return REVIEW_NOT_EXIST_ERROR_MSG + reviewId;
    }
}