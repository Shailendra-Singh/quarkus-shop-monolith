package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerQueryRepository extends PanacheRepository.Reactive.Stateless<Customer, UUID> {
    /**
     * @param enabled: customer status flag
     * @return all enabled customers
     */
    default Uni<List<Customer>> findCustomerByEnabled(Boolean enabled) {
        return list("where enabled = ?1", enabled);
    }
}
