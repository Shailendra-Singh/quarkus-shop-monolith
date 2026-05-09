package me.shail.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.ReviewDto;
import me.shail.models.Review;
import me.shail.repositories.ProductRepository;
import me.shail.repositories.ReviewRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Transactional
@ApplicationScoped
public class ReviewService {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ReviewRepository reviewRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
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

    public Uni<List<ReviewDto>> findReviewsByProductId(UUID productid) {
        log.debug("Request to get all Reviews");
        return this.reviewRepository.findReviewsByProduct_Id(productid)
                .onItem().transform(
                        reviews -> reviews
                                .stream()
                                .map(ReviewService::mapToDto)
                                .toList()
                );
    }

    public Uni<ReviewDto> findById(UUID id) {
        log.debug("Request to get Review: {}", id);
        return this.reviewRepository.findById(id).onItem().transform(ReviewService::mapToDto);
    }

    public Uni<ReviewDto> create(ReviewDto reviewDto, UUID productId) {
        log.debug("Request to create Review: {}", reviewDto);

        return this.productRepository.findById(productId)
                .onItem().ifNull().failWith(
                        () -> new EntityNotFoundException("Product not found. ID: " + productId)
                ).chain(product -> {
                    Review savedReview = new Review(reviewDto.title(),
                            reviewDto.description(),
                            reviewDto.rating(),
                            product);
                    return this.reviewRepository.insert(savedReview)
                            .replaceWith(ReviewService.mapToDto(savedReview));
                });
    }

    public Uni<Boolean> delete(UUID reviewId) {
        log.debug("Request to delete Review: {}", reviewId);
        return this.reviewRepository.findById(reviewId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("Review not found. Id: " + reviewId))
                .chain(review -> {
                    return this.productRepository.findProductByReview_Id(reviewId)
                            .chain(product -> {
                                product.reviews.remove(review);

                                return this.reviewRepository.delete(review).replaceWith(true);
                            });
                });
    }
}
