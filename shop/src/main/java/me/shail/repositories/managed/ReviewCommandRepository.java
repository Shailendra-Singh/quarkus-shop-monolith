package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Review;

import java.util.UUID;

public interface ReviewCommandRepository extends PanacheRepository.Reactive<Review, UUID> {
    /**
     * Deletes all reviews for a product. This does not affect the product object.
     *
     * @param productId product id
     * @return number of reviews affected
     */
    default Uni<Integer> deleteReviewsByProductId(UUID productId) {
        return getSession().chain(session -> {
            String deleteQuery = "DELETE FROM reviews WHERE product_id = :pId";

            return session.createNativeQuery(deleteQuery)
                    .setParameter("pId", productId)
                    .executeUpdate();
        });
    }
}
