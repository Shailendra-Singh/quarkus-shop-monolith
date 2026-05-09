package me.shail.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.ProductDto;
import me.shail.models.Product;
import me.shail.models.enums.ProductStatus;
import me.shail.repositories.CategoryRepository;
import me.shail.repositories.ProductRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@ApplicationScoped
public class ProductService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    ProductRepository productRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CategoryRepository categoryRepository;

    public static ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.id,
                product.name,
                product.description,
                product.price,
                product.status.name(),
                product.salesCounter,
                product.reviews
                        .stream()
                        .map(ReviewService::mapToDto)
                        .collect(Collectors.toUnmodifiableSet()),
                product.category.id

        );
    }

    public Uni<List<ProductDto>> findAll() {
        log.debug("Request to get all products");
        return this.productRepository.findAll()
                .list()
                .onItem()
                .transform(products ->
                        products.stream().map(ProductService::mapToDto).toList()
                );
    }

    public Uni<ProductDto> findById(UUID id) {
        log.debug("Request to get Product: {}", id);
        return this.productRepository.findById(id).onItem().transform(ProductService::mapToDto);
    }

    public Uni<Long> countAll() {
        return this.productRepository.count();
    }

    public Uni<Long> countByCategoryId(UUID categoryId) {
        return this.productRepository.countAllByCategory(categoryId);
    }

    public Uni<ProductDto> create(ProductDto productDto) {
        log.debug("Request to create Product: {}", productDto);
        return this.categoryRepository.findById(productDto.categoryId())
                .onItem().transform(category -> {
                    var product = new Product(
                            productDto.name(),
                            productDto.description(),
                            productDto.price(),
                            ProductStatus.valueOf(productDto.status()),
                            productDto.salesCounter(),
                            Collections.emptySet(),
                            category
                    );
                    return ProductService.mapToDto(product);
                });
    }

    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Product: {}", id);
        return this.productRepository.deleteById(id);
    }

    public Uni<List<ProductDto>> findByCategoryId(UUID categoryId) {
        return this.productRepository.findByCategory_Id(categoryId)
                .onItem()
                .transform(products ->
                        products.stream().map(ProductService::mapToDto).toList()
                );
    }
}