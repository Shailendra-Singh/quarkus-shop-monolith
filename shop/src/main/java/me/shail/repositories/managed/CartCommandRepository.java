package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface CartCommandRepository extends PanacheRepository.Reactive<Cart, UUID> {
    /**
     * @param status: CartStatus
     * @param id:     Cart id
     * @return long: number of rows updated
     */
    default Uni<Long> updateStatusById(CartStatus status, UUID id) {
        return update("set status = ?1 where id = ?2", status, id);
    }

    /**
     * Finds carts by status and the ID of the associated customer.
     *
     * @param status:     CartStatus
     * @param customerId: Customer id associated with this cart
     * @return List<Cart>
     */
    @HQL("from Cart where status = :status and customer.id = :customerId")
    Uni<List<Cart>> findCartByStatusAndCustomerId(CartStatus status, UUID customerId);
}