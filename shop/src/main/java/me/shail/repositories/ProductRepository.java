package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.shail.models.Product;
import me.shail.repositories.managed.ProductCommandRepository;
import me.shail.repositories.stateless.ProductQueryRepository;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ProductRepository {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ProductCommandRepository productCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ProductQueryRepository productQueryRepository;

    public Uni<Product> create(Product product) {
        return this.productCommandRepository.persist(product).replaceWith(product);
    }

    public Uni<Boolean> addCategoryToProduct(UUID productId, UUID categoryId) {
        return this.productCommandRepository.addCategoryToProduct(productId, categoryId);
    }

    public Uni<Boolean> addAllCategoriesToProduct(UUID productId, Set<UUID> categoryIds) {
        return this.productCommandRepository.addAllCategoriesToProduct(productId, categoryIds);
    }

    public Uni<Boolean> removeCategoryFromProduct(UUID productId, UUID categoryId) {
        return this.productCommandRepository.removeCategoryFromProduct(productId, categoryId);
    }

    public Uni<Boolean> deleteById(UUID productId) {
        return this.productCommandRepository.deleteById(productId);
    }

    public Uni<Product> findByIdManaged(UUID productId) {
        return this.productCommandRepository.findByIdWithCategories(productId);
    }

    public Uni<Product> findByIdStateless(UUID productId) {
        return this.productQueryRepository.findByIdWithCategories(productId);
    }

    public Uni<List<Product>> findAll() {
        return this.productQueryRepository.findAllWithCategories();
    }

    public Uni<List<Product>> findByCategoryId(UUID categoryId) {
        return this.productQueryRepository.findByCategoryId(categoryId);
    }

    public Uni<Boolean> existsByIdManaged(UUID productId) {
        return this.productCommandRepository.existById(productId);
    }

    public Uni<Boolean> existsByIdStateless(UUID productId) {
        return this.productQueryRepository.existById(productId);
    }

    public Uni<Mutiny.Session> getSession() {
        return this.productCommandRepository.getSession();
    }

    public Uni<Long> count() {
        return this.productQueryRepository.count();
    }

    public Uni<Long> countAllByCategory(UUID categoryId) {
        return this.productQueryRepository.countAllByCategory(categoryId);
    }
}