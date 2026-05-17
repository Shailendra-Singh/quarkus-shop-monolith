package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import org.hibernate.annotations.processing.HQL;

import java.util.List;
import java.util.UUID;

public interface CartQueryRepository extends PanacheRepository.Reactive.Stateless<Cart, UUID> {

    /**
     * Find carts by status
     *
     * @param status: CartStatus
     * @return List<Cart>
     */
    @HQL("from Cart c left join fetch c.customer")
    Uni<List<Cart>> listAllWithCustomer();

    /**
     * Find carts by status
     *
     * @param status: CartStatus
     * @return List<Cart>
     */
    @HQL("from Cart c left join fetch c.customer where c.status = :status")
    Uni<List<Cart>> findCartByStatus(CartStatus status);

    /**
     * Finds carts by status and the ID of the associated customer.
     *
     * @param status:     CartStatus
     * @param customerId: Customer id associated with this cart
     * @return List<Cart>
     */
    @HQL("from Cart c left join fetch c.customer where c.status = :status and c.customer.id = :customerId")
    Uni<List<Cart>> findCartByStatusAndCustomerId(CartStatus status, UUID customerId);

    /**
     * Finds carts ID. Fetches the customer with eager loading
     *
     * @param cartId: Cart Id
     * @return Cart with eager loading of Customer
     */
    default Uni<Cart> findCartWithCustomer(UUID cartId) {
        // The "JOIN FETCH" forces Hibernate to load the customer in the initial query
        return find("FROM Cart c LEFT JOIN FETCH c.customer WHERE c.id = ?1", cartId)
                .firstResult();
    }

    /**
     * Finds carts ID. Fetches the customer with eager loading
     *
     * @param cartId: Cart Id
     * @return Cart with eager loading of Customer
     */
    default Uni<List<Cart>> findCartsWithCustomer() {
        // The "JOIN FETCH" forces Hibernate to load the customer in the initial query
        return find("FROM Cart c LEFT JOIN FETCH c.customer").list();
    }
}
