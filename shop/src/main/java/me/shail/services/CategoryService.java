package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CategoryDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Category;
import me.shail.repositories.CategoryRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class CategoryService {
    @Inject
    CategoryRepository categoryRepository;

    public static CategoryDto mapToDto(Category category) {
        UUID parentId = category.parent == null ? null : category.parent.id;
        return new CategoryDto(category.id, category.name, category.description, parentId);
    }

    public static Uni<Category> generateUni_FindById(CategoryRepository repository, UUID categoryId, boolean managed) {
        Uni<Category> generatedUni;
        if (managed)
            generatedUni = repository.findByIdManaged(categoryId);
        else
            generatedUni = repository.findByIdStateless(categoryId);

        return generatedUni.onItem()
                .ifNull().failWith(() -> new EntityNotFoundException("Category does not exist. ID: " + categoryId));
    }

    public static Uni<Boolean> generateUni_ExistById(CategoryRepository repository, UUID categoryId, boolean managed) {
        Uni<Boolean> generatedUni;
        if (managed)
            generatedUni = repository.existsByIdManaged(categoryId);
        else
            generatedUni = repository.existsByIdStateless(categoryId);

        return generatedUni;
    }

    @SuppressWarnings("unused")
    public static Uni<Boolean> generateUni_AllExist(CategoryRepository repository,
                                                    Set<UUID> categoryIds,
                                                    boolean managed) {
        Uni<Boolean> generatedUni;
        if (managed)
            generatedUni = repository.allExistManaged(categoryIds);
        else
            generatedUni = repository.allExistStateless((categoryIds));

        return generatedUni;
    }

    @WithCustomStatelessSession
    public Uni<List<CategoryDto>> findAll() {
        log.debug("Request to get all Categories");
        return this.categoryRepository.findAll().onItem()
                .transformToUni(items -> Uni.createFrom()
                        .item(items
                                .stream()
                                .map(CategoryService::mapToDto)
                                .toList()
                        )
                );
    }

    @WithCustomStatelessSession
    public Uni<CategoryDto> findById(UUID categoryId) {
        log.debug("Request to get Category: {}", categoryId);
        return generateUni_FindById(this.categoryRepository, categoryId, false).map(CategoryService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<Boolean> existById(UUID categoryId) {
        log.debug("Request to check if Category exists: {}", categoryId);
        return generateUni_ExistById(this.categoryRepository, categoryId, false);
    }

    @WithTransaction
    public Uni<Integer> removeAllProductsFromCategory(UUID categoryId) {
        return generateUni_ExistById(this.categoryRepository, categoryId, true)
                .chain(exists -> {
                    if (!exists)
                        return Uni.createFrom().failure(() ->
                                new EntityNotFoundException("Category does not exist. ID: " + categoryId)
                        );

                    return this.categoryRepository.removeAllProductsFromCategory(categoryId);
                });
    }

    @WithTransaction
    public Uni<CategoryDto> create(CategoryDto categoryDto, UUID parent_category_id) {
        log.debug("Request to create Category: {}", categoryDto);

        var category = new Category(categoryDto.name(), categoryDto.description());
        if (parent_category_id == null) {
            return this.categoryRepository.create(category).onItem().transform(CategoryService::mapToDto);
        }

        return generateUni_FindById(this.categoryRepository, parent_category_id, true)
                .chain(parentCategory -> {
                    category.parent = parentCategory;
                    return this.categoryRepository.create(category);
                })
                .onItem().transform(CategoryService::mapToDto);
    }
}