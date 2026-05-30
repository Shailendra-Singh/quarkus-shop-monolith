package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Review;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface ReviewQueryRespository extends PanacheRepository.Reactive.Stateless<Review, UUID> {

    /**
     * Finds all reviews for a given product id.
     *
     * @param productId product id
     * @return list of reviews
     */
    @HQL("FROM Review r LEFT JOIN FETCH r.product p where p.id = :productId")
    Uni<List<Review>> findReviewsByProductId(UUID productId);

    /**
     * Counts the number of review for a given product id
     *
     * @param productId product id
     * @return count of reviews associated with a product
     */
    default Uni<Long> countReviewsByProductId(UUID productId) {
        return count("FROM Review r LEFT JOIN FETCH r.product p where p.id = ?1", productId);
    }
}
