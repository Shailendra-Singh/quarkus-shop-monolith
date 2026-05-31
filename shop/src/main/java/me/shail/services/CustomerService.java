package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.CustomerDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Customer;
import me.shail.repositories.CustomerRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class CustomerService {

    public final static String CUSTOMER_NOT_EXIST_ERROR_MSG = "Customer does not exist. ID: ";

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

    @WithCustomStatelessSession
    public Uni<List<CustomerDto>> findAll() {
        log.debug("Request to get all Customers");
        return this.customerRepository.listAll()
                .map(customers -> customers.stream()
                        .map(CustomerService::mapToDto)
                        .toList());
    }

    @WithCustomStatelessSession
    public Uni<CustomerDto> findById(UUID id) {
        log.debug("Request to get Customer: {}", id);
        return this.customerRepository
                .findByIdStateless(id)
                .onItem().ifNull().failWith(() ->
                        new EntityNotFoundException(getCustomerDoesNotExistErrorMessage(id))
                )
                .onItem().transform(CustomerService::mapToDto);
    }

    @WithCustomStatelessSession
    public Uni<List<CustomerDto>> findAllByState(boolean enabled) {
        String status = enabled ? "active" : "inactive";
        log.debug("Request to get all {} customers", status);
        return this.customerRepository.findAllByState(enabled)
                .map(customers -> customers.stream()
                        .map(CustomerService::mapToDto)
                        .toList());
    }

    @WithTransaction
    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Customer: {}", id);
        return this.customerRepository.disableCustomerById(id)
                .onItem().transformToUni(rowsAffected -> {
                    if (rowsAffected == 0)
                        return Uni.createFrom().failure(
                                new EntityNotFoundException(getCustomerDoesNotExistErrorMessage(id))
                        );

                    return Uni.createFrom().item(rowsAffected == 1);
                });
    }

    public static Uni<Customer> generateUni_FindCustomerById(CustomerRepository repository,
                                                             UUID customerId,
                                                             boolean managed) {
        Uni<Customer> generatedUni;
        if (managed)
            generatedUni = repository.findByIdManaged(customerId);
        else
            generatedUni = repository.findByIdStateless(customerId);
        return generatedUni
                .onItem()
                .ifNull()
                .failWith(new EntityNotFoundException(getCustomerDoesNotExistErrorMessage(customerId)));
    }

    public static String getCustomerDoesNotExistErrorMessage(UUID customerId) {
        return CUSTOMER_NOT_EXIST_ERROR_MSG + customerId;
    }
}
