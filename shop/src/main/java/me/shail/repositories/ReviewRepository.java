package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.shail.models.Review;
import me.shail.repositories.managed.ReviewCommandRepository;
import me.shail.repositories.stateless.ReviewQueryRespository;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReviewRepository {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ReviewCommandRepository reviewCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ReviewQueryRespository reviewQueryRespository;

    public Uni<Mutiny.Session> getSession() {
        return this.reviewCommandRepository.getSession();
    }

    public Uni<Review> create(Review review) {
        return this.reviewCommandRepository.persist(review).replaceWith(review);
    }

    public Uni<Boolean> deleteById(UUID reviewId) {
        return this.reviewCommandRepository.deleteById(reviewId);
    }

    public Uni<Integer> deleteReviewsByProductId(UUID productId) {
        return this.reviewCommandRepository.deleteReviewsByProductId(productId);
    }

    public Uni<Review> findByIdManaged(UUID reviewId) {
        return this.reviewCommandRepository.findById(reviewId);
    }

    public Uni<Review> findByIdStateless(UUID reviewId) {
        return this.reviewQueryRespository.findById(reviewId);
    }

    public Uni<Boolean> existByIdManaged(UUID reviewId) {
        return this.reviewCommandRepository.existById(reviewId);
    }

    public Uni<Boolean> existByIdStateless(UUID reviewId) {
        return this.reviewQueryRespository.existById(reviewId);
    }

    public Uni<List<Review>> findReviewsByProductId(UUID productId) {
        return this.reviewQueryRespository.findReviewsByProductId(productId);
    }

    public Uni<Long> countReviewsByProductId(UUID productId) {
        return this.reviewQueryRespository.countReviewsByProductId(productId);
    }
}