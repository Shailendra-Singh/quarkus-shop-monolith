package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.ProductDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Product;
import me.shail.models.enums.ProductStatus;
import me.shail.repositories.ProductRepository;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class ProductService {
    @Inject
    ProductRepository productRepository;

    public static ProductDto mapToDto(Product product, Set<UUID> productCategoryIds) {
        return new ProductDto(
                product.id,
                product.name,
                product.description,
                product.price,
                product.status.name(),
                product.salesCounter,
                productCategoryIds
        );
    }

    public static ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.id,
                product.name,
                product.description,
                product.price,
                product.status.name(),
                product.salesCounter,
                product.categories
                        .stream()
                        .map(c -> c.id)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    public static Uni<Product> generateUni_FindById(ProductRepository repository, UUID productId, boolean managed) {
        Uni<Product> generatedUni;
        if (managed)
            generatedUni = repository.findByIdManaged(productId);
        else
            generatedUni = repository.findByIdStateless(productId);

        return generatedUni.onItem().ifNull()
                .failWith(() -> new EntityNotFoundException("Product does not exist. ID: " + productId));
    }

    public static Uni<Boolean> generateUni_ExistById(ProductRepository repository, UUID productId, boolean managed) {
        Uni<Boolean> generatedUni;
        if (managed)
            generatedUni = repository.existsByIdManaged(productId);
        else
            generatedUni = repository.existsByIdStateless(productId);

        return generatedUni;
    }

    @WithCustomStatelessSession
    public Uni<List<ProductDto>> findAll() {
        log.debug("Request to get all products");
        return this.productRepository.findAll()
                .map(products -> products
                        .stream()
                        .map(ProductService::mapToDto)
                        .toList());
    }

    @WithCustomStatelessSession
    public Uni<ProductDto> findById(UUID productId) {
        log.debug("Request to get Product: {}", productId);
        return generateUni_FindById(this.productRepository, productId, false)
                .map(ProductService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<Boolean> existById(UUID productId) {
        log.debug("Request to check if Product exists: {}", productId);
        return generateUni_ExistById(this.productRepository, productId, false);
    }

    @WithCustomStatelessSession
    public Uni<Long> countAll() {
        return this.productRepository.count();
    }

    @WithCustomStatelessSession
    public Uni<Long> countByCategoryId(UUID categoryId) {
        return this.productRepository.countAllByCategory(categoryId);
    }

    @WithTransaction
    public Uni<ProductDto> create(ProductDto productDto) {
        log.debug("Request to create Product: {}", productDto);

        var product = new Product(
                productDto.name(),
                productDto.description(),
                productDto.price(),
                ProductStatus.valueOf(productDto.status()),
                productDto.salesCounter()
        );
        if (productDto.categories() == null || productDto.categories().isEmpty()) {
            return this.productRepository.create(product)
                    .map(p -> ProductService.mapToDto(p, Collections.emptySet()));
        }

        return this.productRepository.create(product)
                .call(() -> productRepository.getSession().chain(Mutiny.Session::flush))
                .chain(createdProduct ->
                        this.addAllCategoriesToProduct(createdProduct.id, productDto.categories())
                                .replaceWith(createdProduct)
                ).map(p -> ProductService.mapToDto(p, productDto.categories()));
    }

    @WithTransaction
    public Uni<Boolean> addCategoryToProduct(UUID productId, UUID categoryId) {
        return this.productRepository.addCategoryToProduct(productId, categoryId);
    }

    @WithTransaction
    public Uni<Boolean> addAllCategoriesToProduct(UUID productId, Set<UUID> categoryIds) {
        return this.productRepository.addAllCategoriesToProduct(productId, categoryIds);
    }

    @WithTransaction
    public Uni<Boolean> removeCategoryFromProduct(UUID productId, UUID categoryId) {
        return generateUni_ExistById(this.productRepository, productId, true)
                .chain(exists -> {
                    if (exists)
                        return this.productRepository.removeCategoryFromProduct(productId, categoryId);
                    else
                        return Uni.createFrom().failure(
                                () -> new EntityNotFoundException("Product does not exist. ID: " + productId)
                        );
                });
    }

    @WithTransaction
    public Uni<Boolean> deleteById(UUID productId) {
        log.debug("Request to delete Product: {}", productId);
        return this.productRepository.deleteById(productId);
    }

    @WithCustomStatelessSession
    public Uni<List<ProductDto>> findByCategoryId(UUID categoryId) {
        return this.productRepository.findByCategoryId(categoryId)
                .map(products -> products
                        .stream()
                        .map(ProductService::mapToDto).toList()
                );
    }
}