package me.shail.repositories;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.shail.models.Customer;
import me.shail.repositories.managed.CustomerCommandRepository;
import me.shail.repositories.stateless.CustomerQueryRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CustomerRepository {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CustomerCommandRepository customerCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CustomerQueryRepository customerQueryRepository;

    public Uni<Customer> create(Customer customer) {
        return customerCommandRepository.persist(customer).replaceWith(customer);
    }

    public Uni<Boolean> deleteById(UUID customerId) {
        return customerCommandRepository.deleteById(customerId);
    }

    public Uni<Long> disableCustomerById(UUID customerId) {
        return customerCommandRepository.disableCustomerById(customerId);
    }

    public Uni<List<Customer>> listAll() {
        return customerQueryRepository.listAll();
    }

    public Uni<Customer> findByIdStateless(UUID customerId) {
        return customerQueryRepository.findById(customerId);
    }

    public Uni<Customer> findByIdManaged(UUID customerId) {
        return customerCommandRepository.findById(customerId);
    }

    public Uni<List<Customer>> findAllByState(boolean enabled) {
        return customerQueryRepository.findCustomerByEnabled(enabled);
    }
}
