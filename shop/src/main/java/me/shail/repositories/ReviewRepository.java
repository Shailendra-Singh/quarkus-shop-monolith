package me.shail.repositories;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import me.shail.models.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends PanacheRepositoryBase<Review, UUID> {
    default Uni<List<Review>> findReviewsByProductId(UUID productId) {
        return list("product.id = ?1", productId);
    }
}
