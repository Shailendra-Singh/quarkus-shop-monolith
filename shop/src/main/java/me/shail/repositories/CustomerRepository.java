package me.shail.repositories;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Customer;
import org.hibernate.annotations.processing.Find;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends PanacheRepository.Reactive.Stateless<Customer, UUID> {
    @Find
    Uni<List<Customer>> findAllByEnabled(Boolean enabled);
}
