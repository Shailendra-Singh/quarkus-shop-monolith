package me.shail.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CartDto;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.CustomerRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@Transactional
public class CartService {
    //    @Inject
    CartRepository cartRepository;

    //    @Inject
    CustomerRepository customerRepository;

    public static CartDto mapToDto(Cart cart) {
        return new CartDto(cart.id,
                CustomerService.mapToDto(cart.customer),
                cart.status.name()
        );
    }

    public Uni<List<CartDto>> listAll() {
        log.debug("Request to get all Carts");
        return this.cartRepository.listAll()
                .onItem()
                .transform(carts -> carts.stream()
                        .map(CartService::mapToDto)
                        .collect(Collectors.toList())
                );
    }

    public Uni<List<CartDto>> findAllActiveCarts() {
        log.debug("Request to get all 'active' Carts");
        return this.cartRepository.findByStatus(CartStatus.NEW)
                .onItem()
                .transform(carts -> carts.stream()
                        .map(CartService::mapToDto)
                        .collect(Collectors.toList()));
    }

    public Uni<Cart> create(UUID customerId) {
        return this.getActiveCart(customerId)
                .onItem().ifNotNull().failWith(new IllegalStateException("Active cart already exists."))
                .chain(() -> this.customerRepository.findById(customerId))
                .onItem().ifNull().failWith(new IllegalStateException("The Customer does not exist!"))
                .chain(customer -> {
                    var cart = new Cart(customer, CartStatus.NEW);
                    return this.cartRepository.insert(cart).replaceWith(cart);
                });
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Uni<CartDto> findById(UUID id) {
        log.debug("Request to get Cart: {}", id);
        return this.cartRepository.findById(id)
                .onItem()
                .transform(CartService::mapToDto);
    }

    public Uni<Integer> delete(UUID id) {
        log.debug("Request to delete Cart: {}", id);
        return this.cartRepository.updateStatusById(id, CartStatus.CANCELED)
                .onItem().transformToUni(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Cannot delete: No Cart found with id:" + id)
                        );

                    return Uni.createFrom().item(rowsAffected);
                });
    }

    public Uni<Cart> getActiveCart(UUID customerId) {
        return this.cartRepository.findByStatusAndCustomer_Id(CartStatus.NEW, customerId)
                .onItem().transformToUni(carts -> {
                    if (carts == null || carts.isEmpty())
                        return Uni.createFrom().nullItem();

                    if (carts.size() == 1)
                        return Uni.createFrom().item(carts.getFirst());

                    return Uni.createFrom().failure(
                            new IllegalStateException("Multiple active carts detected for customer: " + customerId)
                    );
                });
    }
}