package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Category;

import java.util.Set;
import java.util.UUID;

public interface CategoryQueryRepository extends PanacheRepository.Reactive.Stateless<Category, UUID> {
    /**
     * Checks if category exists
     *
     * @param categoryId category id
     * @return True/False
     */
    default Uni<Boolean> existById(UUID categoryId) {
        return count("id = ?1", categoryId)
                .map(count -> count == 1);
    }

    /**
     * Checks if the list of Category Ids exist
     *
     * @param categoryIds list of category ids
     * @return True if all exist else false
     */
    default Uni<Boolean> allExist(Set<UUID> categoryIds) {
        return count("WHERE id in ?1", categoryIds)
                .map(count -> count == categoryIds.size());
    }
}