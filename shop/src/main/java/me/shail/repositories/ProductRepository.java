package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Product;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends PanacheRepository.Reactive.Stateless<Product, UUID> {
    @Find
    Uni<List<Product>> findByCategory_Id(UUID id);

    @HQL("from Product p join p.reviews r where r.id = :reviewId")
    Uni<Product> findProductByReview_Id(UUID reviewId);

    @HQL("delete from Product where category.id = :categoryId")
    Uni<Integer> deleteByCategoryId(UUID categoryId);
//
//    @HQL("from Product where category.id = :categoryId")
//    Uni<List<Product>> findByCategory(UUID categoryId);

    @HQL("select count(distinct p.id) from Product p where p.category.id = :categoryId")
    Uni<Long> countAllByCategory(UUID categoryId);
}
