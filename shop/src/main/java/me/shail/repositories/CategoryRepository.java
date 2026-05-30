package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.shail.models.Category;
import me.shail.repositories.managed.CategoryCommandRepository;
import me.shail.repositories.stateless.CategoryQueryRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CategoryRepository {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CategoryCommandRepository categoryCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CategoryQueryRepository categoryQueryRepository;

    public Uni<Category> create(Category category) {
        return this.categoryCommandRepository.persist(category).replaceWith(category);
    }

    public Uni<Integer> removeAllProductsFromCategory(UUID categoryId) {
        return this.categoryCommandRepository.removeAllProductsFromCategory(categoryId);
    }

    public Uni<List<Category>> findAll() {
        return this.categoryQueryRepository.listAll();
    }

    public Uni<Category> findByIdStateless(UUID categoryId) {
        return this.categoryQueryRepository.findById(categoryId);
    }

    public Uni<Category> findByIdManaged(UUID categoryId) {
        return this.categoryCommandRepository.findById(categoryId);
    }

    public Uni<Boolean> existsByIdManaged(UUID categoryId) {
        return this.categoryCommandRepository.existById(categoryId);
    }

    public Uni<Boolean> existsByIdStateless(UUID categoryId) {
        return this.categoryQueryRepository.existById(categoryId);
    }

    public Uni<Boolean> allExistManaged(Set<UUID> categoryIds) {
        return this.categoryCommandRepository.allExist(categoryIds);
    }

    public Uni<Boolean> allExistStateless(Set<UUID> categoryIds) {
        return this.categoryQueryRepository.allExist(categoryIds);
    }
}