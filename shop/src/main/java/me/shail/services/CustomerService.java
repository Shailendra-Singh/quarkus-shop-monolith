package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CustomerDto;
import me.shail.models.Customer;
import me.shail.repositories.CustomerRepository;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class CustomerService {
    @Inject
    CustomerRepository customerRepository;

    public static CustomerDto mapToDto(Customer customer) {
        return new CustomerDto(
                customer.id,
                customer.firstName,
                customer.lastName,
                customer.email,
                customer.telephone
        );
    }

    @WithTransaction
    public Uni<CustomerDto> create(CustomerDto customerDto) {
        log.debug("Request to create Customer: {}", customerDto);
        var customer = new Customer(
                customerDto.firstName(),
                customerDto.lastName(),
                customerDto.email(),
                customerDto.telephone(),
                Collections.emptySet(),
                Boolean.TRUE
        );


        return this.customerRepository.create(customer)
                .onItem()
                .transform(CustomerService::mapToDto);
    }

    public Uni<List<CustomerDto>> findAll() {
        log.debug("Request to get all Customers");
        return this.customerRepository.listAll()
                .onItem()
                .transformToUni(customers -> Uni.createFrom()
                        .item(customers.stream()
                                .map(CustomerService::mapToDto)
                                .toList()
                        )
                );
    }

    public Uni<CustomerDto> findById(UUID id) {
        log.debug("Request to get Customer: {}", id);
        return this.customerRepository.findById(id).onItem().transform(CustomerService::mapToDto);
    }

    public Uni<List<CustomerDto>> findAllByState(boolean enabled) {
        String status = enabled ? "active" : "inactive";
        log.debug("Request to get all {} customers", status);
        return this.customerRepository.findAllByState(enabled)
                .onItem()
                .transformToUni(customers ->
                        Uni.createFrom()
                                .item(customers
                                        .stream()
                                        .map(CustomerService::mapToDto)
                                        .toList()
                                )
                );
    }

    @WithTransaction
    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Customer: {}", id);
        return this.customerRepository.disableCustomerById(id)
                .onItem().transformToUni(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Cannot find Customer with id " + id)
                        );

                    return Uni.createFrom().item(rowsAffected == 1);
                });
    }
}
