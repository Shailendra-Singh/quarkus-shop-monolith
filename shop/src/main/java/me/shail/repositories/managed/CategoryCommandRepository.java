package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Category;
import me.shail.models.Product;

import java.util.Set;
import java.util.UUID;

public interface CategoryCommandRepository extends PanacheRepository.Reactive<Category, UUID> {

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

    /**
     * Idempotent operation: Removes or unlinks a category from all products
     *
     * @param categoryId category id of category to be unlinked/removed
     * @return number of rows affected
     */
    default Uni<Integer> removeAllProductsFromCategory(UUID categoryId) {
        return getSession().chain(session -> {
            String query = "DELETE FROM " + Product.TABLE_PRODUCT_CATEGORIES + " " +
                    "WHERE category_id = :cId";
            return session.createNativeQuery(query).setParameter("cId", categoryId)
                    .executeUpdate();
        });
    }
}