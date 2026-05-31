package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CartDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Cart;
import me.shail.models.enums.CartStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.CustomerRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class CartService {

    public final static String CART_NOT_EXIST_ERROR_MSG = "Cart does not exist. ID: ";
    public final static String ACTIVE_CART_EXIST_ERROR_MSG = "Active cart already exists.";
    public final static String MULTIPLE_ACTIVE_CART_EXIST_ERROR_MSG = "Multiple active carts detected for customer: ";

    @Inject
    CartRepository cartRepository;

    @Inject
    CustomerRepository customerRepository;

    public static CartDto mapToDto(Cart cart) {
        return new CartDto(cart.id,
                CustomerService.mapToDto(cart.customer),
                cart.status.name()
        );
    }

    @WithCustomStatelessSession
    public Uni<List<CartDto>> listAll() {
        log.debug("Request to get all Carts");
        return this.cartRepository.listAll()
                .onItem()
                .transform(carts -> carts.stream()
                        .map(CartService::mapToDto)
                        .collect(Collectors.toList())
                );
    }

    @WithCustomStatelessSession
    public Uni<List<CartDto>> findAllActiveCarts() {
        log.debug("Request to get all 'active' Carts");
        return this.cartRepository.findCartByStatus(CartStatus.NEW)
                .onItem()
                .transform(carts -> carts.stream()
                        .map(CartService::mapToDto)
                        .collect(Collectors.toList()));
    }

    @WithTransaction
    public Uni<CartDto> create(UUID customerId) {
        return CustomerService.generateUni_FindCustomerById(this.customerRepository, customerId, true)
                .chain(customer ->
                        generateUni_FindCartByStatusAndCustomer(customer.id, true)
                                .onItem().ifNotNull().failWith(() -> new IllegalStateException(ACTIVE_CART_EXIST_ERROR_MSG))
                                .chain(_ -> {
                                    var cart = new Cart(customer, CartStatus.NEW);
                                    return this.cartRepository.create(cart);
                                }).chain(cart -> {
                                    var customerDto = CustomerService.mapToDto(customer);
                                    var cartDto = new CartDto(cart.id, customerDto, cart.status.name());
                                    return Uni.createFrom().item(cartDto);
                                }));
    }

    @WithCustomStatelessSession
    public Uni<CartDto> findById(UUID id) {
        log.debug("Request to get Cart: {}", id);
        return generateUni_FindCartWithCustomer(this.cartRepository, id, false)
                .onItem().transform(CartService::mapToDto);
    }

    @WithTransaction
    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Cart: {}", id);
        return this.cartRepository.updateStatusById(CartStatus.CANCELED, id)
                .onItem().transformToUni(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Uni.createFrom().failure(
                                new EntityNotFoundException(getCartDoesNotExistErrorMessage(id))
                        );

                    return Uni.createFrom().item(rowsAffected > 0);
                });
    }

    @WithCustomStatelessSession
    public Uni<CartDto> getActiveCart(UUID customerId) {
        return CustomerService.generateUni_FindCustomerById(this.customerRepository, customerId, false)
                .chain(_ ->
                        generateUni_FindCartByStatusAndCustomer(customerId, false))
                .map(CartService::mapToDto);
    }

    public static Uni<Cart> generateUni_FindCartWithCustomer(CartRepository repository, UUID cartId, boolean managed) {
        Uni<Cart> generatedUni;
        if (managed)
            generatedUni = repository.findCartWithCustomerManaged(cartId);
        else
            generatedUni = repository.findCartWithCustomerStateless(cartId);

        return generatedUni.onItem().ifNull().failWith(() ->
                new EntityNotFoundException(getCartDoesNotExistErrorMessage(cartId))
        );
    }

    private Uni<Cart> generateUni_FindCartByStatusAndCustomer(UUID customerId, boolean managed) {
        Uni<List<Cart>> generatedUni;
        if (managed)
            generatedUni = this.cartRepository
                    .findCartByStatusAndCustomerIdManaged(CartStatus.NEW, customerId);
        else
            generatedUni = this.cartRepository
                    .findCartByStatusAndCustomerIdStateless(CartStatus.NEW, customerId);
        return generatedUni
                .onItem().transformToUni(carts -> {
                    if (carts == null || carts.isEmpty())
                        return Uni.createFrom().nullItem();

                    if (carts.size() == 1)
                        return Uni.createFrom().item(carts.getFirst());

                    return Uni.createFrom().failure(
                            new IllegalStateException(getMultipleActiveCartExistErrorMsg(customerId))
                    );
                });
    }

    public static String getMultipleActiveCartExistErrorMsg(UUID customerId) {
        return MULTIPLE_ACTIVE_CART_EXIST_ERROR_MSG + customerId;
    }

    public static String getCartDoesNotExistErrorMessage(UUID cartId) {
        return CART_NOT_EXIST_ERROR_MSG + cartId;
    }
}