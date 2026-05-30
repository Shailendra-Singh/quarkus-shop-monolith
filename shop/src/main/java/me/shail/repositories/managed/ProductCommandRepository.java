package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Product;

import javax.naming.OperationNotSupportedException;
import java.util.Set;
import java.util.UUID;

public interface ProductCommandRepository extends PanacheRepository.Reactive<Product, UUID> {

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
     * Adds category to a product
     *
     * @param productId  product id of product to be modified
     * @param categoryId category id of category to be added
     * @return True: if it doesn't exist, False: if already exist
     */
    default Uni<Boolean> addCategoryToProduct(UUID productId, UUID categoryId) {
        return getSession().chain(session -> {
            String insertQuery = "INSERT INTO " + Product.TABLE_PRODUCT_CATEGORIES + " (product_id, category_id) " +
                    "VALUES (:pId, :cId) " +
                    "ON CONFLICT (product_id, category_id) " +
                    "DO NOTHING";

            String errorMessage = String.format("FK Constraint Violation. " +
                    "Invalid Product (Id: %s) or Category (Id: %s)", productId, categoryId);

            return session.createNativeQuery(insertQuery)
                    .setParameter("pId", productId)
                    .setParameter("cId", categoryId)
                    .executeUpdate()
                    .map(rowsUpdated -> rowsUpdated == 1)
                    .onFailure().transform(err -> {
                        if (err.getMessage().toLowerCase().contains("violates foreign key"))
                            return new OperationNotSupportedException(errorMessage);
                        return err;
                    });
        });
    }

    /**
     * Adds category to a product
     *
     * @param productId   product id of product to be modified
     * @param categoryIds collection of category ids to be added
     * @return True: if it doesn't exist, False: if already exist
     */
    default Uni<Boolean> addAllCategoriesToProduct(UUID productId, Set<UUID> categoryIds) {
        return getSession().chain(session -> {
            String insertQuery =
                    "INSERT INTO " + Product.TABLE_PRODUCT_CATEGORIES + " (product_id, category_id) " +
                            "SELECT :pId, unnest(CAST(:cIds AS uuid[])) " +
                            "ON CONFLICT (product_id, category_id) DO NOTHING";

            String errorMessage = "FK Constraint Violation. Invalid Product Id or one or more Category Ids.";

            return session.createNativeQuery(insertQuery)
                    .setParameter("pId", productId)
                    .setParameter("cIds", categoryIds.toArray(new UUID[0]))
                    .executeUpdate()
                    .map(rowsUpdated -> rowsUpdated == categoryIds.size())
                    .onFailure().transform(err -> {
                        if (err.getMessage().toLowerCase().contains("violates foreign key"))
                            return new OperationNotSupportedException(errorMessage);
                        return err;
                    });
        });
    }

    /**
     * Idempotent operation: Removes or unlinks a category from a product
     *
     * @param productId  product id of product to be modified
     * @param categoryId category id of category to be unlinked/removed
     * @return True: if any rows were updated, False: if no rows updated or there was no need to do so
     */
    default Uni<Boolean> removeCategoryFromProduct(UUID productId, UUID categoryId) {
        return getSession().chain(session -> {
            String deleteQuery = "DELETE FROM " + Product.TABLE_PRODUCT_CATEGORIES +
                    " WHERE product_id = :pId AND category_id = :cId ";

            return session.createNativeQuery(deleteQuery)
                    .setParameter("pId", productId)
                    .setParameter("cId", categoryId)
                    .executeUpdate()
                    .map(rowsUpdated -> rowsUpdated == 1);
        });
    }
}