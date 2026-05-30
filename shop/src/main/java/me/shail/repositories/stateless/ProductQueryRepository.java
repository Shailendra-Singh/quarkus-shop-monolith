package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Product;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface ProductQueryRepository extends PanacheRepository.Reactive.Stateless<Product, UUID> {

    /**
     * Checks if a product exists
     *
     * @param productId product id
     * @return True/False
     */
    default Uni<Boolean> existById(UUID productId) {
        return count("id = ?1", productId)
                .map(count -> count == 1);
    }

    /**
     * Finds the product with nested categories set
     *
     * @param productId product id
     * @return Product
     */
    default Uni<Product> findByIdWithCategories(UUID productId) {
        return find("FROM Product p LEFT JOIN FETCH p.categories where p.id =?1", productId)
                .firstResult();
    }

    /**
     * Finds all products with nested categories set
     *
     * @return List of products
     */
    @HQL("FROM Product p LEFT JOIN FETCH p.categories")
    Uni<List<Product>> findAllWithCategories();

    /**
     * Finds products by category id
     *
     * @param categoryId category id
     * @return List of products
     */
    @HQL("FROM Product p LEFT JOIN FETCH p.categories JOIN p.categories c WHERE c.id = :categoryId")
    Uni<List<Product>> findByCategoryId(UUID categoryId);


    /**
     * Counts the number of products within a category
     *
     * @param categoryId category id
     * @return count of products
     */
    @HQL("select count(distinct p.id) from Product p JOIN p.categories c WHERE c.id = :categoryId")
    Uni<Long> countAllByCategory(UUID categoryId);
}