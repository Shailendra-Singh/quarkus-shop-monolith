package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Customer;

import java.util.UUID;

public interface CustomerCommandRepository extends PanacheRepository.Reactive<Customer, UUID>{
    /**
     * Disables a customer based on 'enabled' flag
     *
     * @param customerId: Customer id
     * @return count of rows affected
     */
    default Uni<Long> disableCustomerById(UUID customerId) {
        return update("set enabled = false where id = ?1", customerId);
    }
}
