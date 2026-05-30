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
        return this.reviewRepository.countReviewsByProductId(productId);
    }

    @WithCustomStatelessSession
    public Uni<List<ReviewDto>> findReviewsByProductId(UUID productId) {
        log.debug("Request to get all Reviews");
        return this.reviewRepository.findReviewsByProductId(productId)
                .map(reviews -> reviews.stream()
                        .map(ReviewService::mapToDto)
                        .toList());
    }

    @WithCustomStatelessSession
    public Uni<ReviewDto> findById(UUID reviewId) {
        log.debug("Request to get Review: {}", reviewId);
        return this.reviewRepository.findById(reviewId)
                .onItem().ifNull().failWith(() ->
                        new EntityNotFoundException("Review does not exist. Id: " + reviewId)
                )
                .map(ReviewService::mapToDto);
    }

    @WithTransaction
    public Uni<ReviewDto> create(ReviewDto reviewDto, UUID productId) {
        log.debug("Request to create Review: {} for Product: {}", reviewDto, productId);

        return ProductService
                .generateUni_ExistById(this.productRepository, productId, true)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException("Product does not exist. ID: " + productId)
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
        return this.reviewRepository.deleteById(reviewId);
    }

    @WithTransaction
    public Uni<Integer> deleteAllReviewsByProductId(UUID productId) {
        log.debug("Request to delete all reviews for Product Id: {}", productId);
        return this.reviewRepository.deleteReviewsByProductId(productId);
    }
}