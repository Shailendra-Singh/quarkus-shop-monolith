package me.shail.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CartDto;
import me.shail.models.Cart;
import me.shail.models.Customer;
import me.shail.models.enums.CartStatus;
import me.shail.repositories.CartRepository;
import me.shail.repositories.CustomerRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class CartService {
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
        return this.cartRepository.findCartByStatus(CartStatus.NEW)
                .onItem()
                .transform(carts -> carts.stream()
                        .map(CartService::mapToDto)
                        .collect(Collectors.toList()));
    }

    public Uni<CartDto> create(UUID customerId) {
        return generateUni_FindCustomerById(customerId, true)
                .chain(customer ->
                        generateUni_FindCartByStatusAndCustomer(customer.id, true)
                                .onItem().ifNotNull().failWith(() -> new IllegalStateException("Active cart already exists."))
                                .chain(_ -> {
                                    var cart = new Cart(customer, CartStatus.NEW);
                                    return this.cartRepository.create(cart);
                                }).chain(cart -> {
                                    var customerDto = CustomerService.mapToDto(customer);
                                    var cartDto = new CartDto(cart.id, customerDto, cart.status.name());
                                    return Uni.createFrom().item(cartDto);
                                }));
    }

    public Uni<CartDto> findById(UUID id) {
        log.debug("Request to get Cart: {}", id);
        return this.cartRepository.findCartWithCustomer(id)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("The Cart does not exist!"))
                .onItem().transform(CartService::mapToDto);
    }

    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Cart: {}", id);
        return this.cartRepository.updateStatusById(CartStatus.CANCELED, id)
                .onItem().transformToUni(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Uni.createFrom().failure(
                                new EntityNotFoundException("Cannot delete: No Cart found with id:" + id)
                        );

                    return Uni.createFrom().item(rowsAffected > 0);
                });
    }

    public Uni<CartDto> getActiveCart(UUID customerId, boolean managed) {
        return genearateUni_GetActiveCart(customerId, managed).onItem()
                .transform(CartService::mapToDto);
    }

    private Uni<Cart> genearateUni_GetActiveCart(UUID customerId, boolean managed) {
        return generateUni_FindCustomerById(customerId, managed)
                .chain(_ ->
                        generateUni_FindCartByStatusAndCustomer(customerId, managed));
    }

    private Uni<Customer> generateUni_FindCustomerById(UUID customerId, boolean managed) {
        Uni<Customer> generatedUni;
        if (managed)
            generatedUni = this.customerRepository.findByIdManaged(customerId);
        else
            generatedUni = this.customerRepository.findByIdStateless(customerId);
        return generatedUni
                .onItem()
                .ifNull()
                .failWith(new EntityNotFoundException("The Customer does not exist!"));
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
                            new IllegalStateException("Multiple active carts detected for customer: " + customerId)
                    );
                });
    }
}