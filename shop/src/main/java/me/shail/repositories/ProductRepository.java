package me.shail.repositories;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import me.shail.models.Product;
import org.hibernate.annotations.processing.Find;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends PanacheRepositoryBase<Product, UUID> {
    @Find
    Uni<List<Product>> findByCategory_Id(UUID id);

    Uni<Long> countByCategoryId(UUID id);

    default Uni<Product> findProductByReview_Id(UUID reviewId) {
        return find("from Product p join p.reviews r where r.id = ?1", reviewId).firstResult();
    }

    default Uni<Long> deleteAllByCategory_Id(UUID categoryId) {
        return delete("category.id = ?1", categoryId);
    }

    @Find
    Uni<List<Product>> findAllByCategory_Id(UUID id);
}
