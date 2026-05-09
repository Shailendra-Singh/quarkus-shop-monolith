package me.shail.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CategoryDto;
import me.shail.dtos.ProductDto;
import me.shail.models.Category;
import me.shail.repositories.CategoryRepository;
import me.shail.repositories.ProductRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Slf4j
@Transactional
public class CategoryService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CategoryRepository categoryRepository;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ProductRepository productRepository;

    public static CategoryDto mapToDto(Category category, Long productsCount) {
        return new CategoryDto(category.id, category.name, category.description, productsCount);
    }

    public Uni<List<CategoryDto>> findAll() {
        log.debug("Request to get all Categories");
        return this.categoryRepository.findAllCategoryDtos();
    }

    public Uni<CategoryDto> findById(UUID id) {
        log.debug("Request to get Category: {}", id);
        return this.categoryRepository.findCategoryDtoById(id);
    }

    public Uni<CategoryDto> create(CategoryDto categoryDto) {
        log.debug("Request to create Category: {}", categoryDto);
        var category = new Category(categoryDto.name(), categoryDto.description());
        return this.categoryRepository
                .insert(category)
                .replaceWith(category)
                .onItem().transformToUni(c -> {
                    return Uni.createFrom().item(CategoryService.mapToDto(c, 0L));
                });
    }

    public Uni<Integer> delete(UUID id) {
        log.debug("Request to delete Category: {}", id);
        log.debug("Deleting all products for the Category: {}", id);
        return this.productRepository.deleteByCategoryId(id);
    }

    public Uni<List<ProductDto>> findProductsByCategoryId(UUID id) {
        return this.productRepository.findByCategory_Id(id)
                .onItem()
                .transformToUni(products -> {
                    return Uni.createFrom().item(products.stream().map(ProductService::mapToDto).toList());
                });
    }
}
