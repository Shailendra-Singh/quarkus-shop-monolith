package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import me.shail.repositories.managed.CartCommandRepository;
import me.shail.repositories.stateless.CartQueryRepository;

import java.util.List;
import java.util.UUID;

@Dependent
public class CartRepository {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CartCommandRepository cartCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CartQueryRepository cartQueryRepository;

    public Uni<Cart> create(Cart cart) {
        return cartCommandRepository.persist(cart).replaceWith(cart);
    }

    public Uni<Boolean> delete(UUID id) {
        return cartCommandRepository.deleteById(id);
    }

    public Uni<Long> updateStatusById(CartStatus cartStatus, UUID cartId) {
        return cartCommandRepository.updateStatusById(cartStatus, cartId);
    }

    public Uni<List<Cart>> listAll() {
        return cartQueryRepository.listAllWithCustomer();
    }

    public Uni<Cart> findByIdStateless(UUID customerId) {
        return cartQueryRepository.findById(customerId);
    }

    public Uni<Cart> findByIdManaged(UUID customerId) {
        return cartCommandRepository.findById(customerId);
    }

    public Uni<Cart> findCartWithCustomer(UUID cartId) {
        return cartQueryRepository.findCartWithCustomer(cartId);
    }

    public Uni<List<Cart>> findCartByStatus(CartStatus status) {
        return cartQueryRepository.findCartByStatus(status);
    }

    public Uni<List<Cart>> findCartByStatusAndCustomerIdStateless(CartStatus cartStatus, UUID customerId) {
        return cartQueryRepository.findCartByStatusAndCustomerId(cartStatus, customerId);
    }

    public Uni<List<Cart>> findCartByStatusAndCustomerIdManaged(CartStatus cartStatus, UUID customerId) {
        return cartCommandRepository.findCartByStatusAndCustomerId(cartStatus, customerId);
    }
}