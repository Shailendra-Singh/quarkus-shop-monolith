package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Review;
import org.hibernate.annotations.processing.Find;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends PanacheRepository.Reactive.Stateless<Review, UUID> {
    @Find
    Uni<List<Review>> findReviewsByProduct_Id(UUID product$id);
}
