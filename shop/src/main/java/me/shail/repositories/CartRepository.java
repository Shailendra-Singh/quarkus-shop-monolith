package me.shail.repositories;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import org.hibernate.annotations.processing.Find;

import java.util.List;
import java.util.UUID;

public interface CartRepository extends PanacheRepositoryBase<Cart, UUID> {
    /**
     * Finds carts by status.
     * Hibernate maps 'status' parameter to Cart.status field.
     */
    @Find
    Uni<List<Cart>> findByStatus(CartStatus status);

    /**
     * Finds carts by status and the ID of the associated customer.
     */
    @Find
    Uni<List<Cart>> findByStatusAndCustomer_Id(CartStatus status, UUID id);
}