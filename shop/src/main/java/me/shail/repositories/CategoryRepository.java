package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.dtos.CategoryDto;
import me.shail.models.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends PanacheRepository.Reactive.Stateless<Category, UUID> {
    default Uni<List<CategoryDto>> findAllCategoryDtos() {
        return find("SELECT c.id, c.name, c.description, "
                + "(SELECT count(p) FROM Product p WHERE p.category = c) as productsCount"
                + "FROM Category c")
                .project(CategoryDto.class)
                .list();
    }

    default Uni<CategoryDto> findCategoryDtoById(UUID categoryId) {
        return find("SELECT c.id, c.name, c.description, "
                + "(SELECT count(p) FROM Product p WHERE p.category = c) as productsCount"
                + "FROM Category c"
                + "WHERE c.id = :categoryId", categoryId)
                .project(CategoryDto.class).firstResult();
    }
}