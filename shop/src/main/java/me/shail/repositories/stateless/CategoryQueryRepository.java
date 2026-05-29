package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import me.shail.models.Category;

import java.util.UUID;

public interface CategoryQueryRepository extends PanacheRepository.Reactive.Stateless<Category, UUID> {

}
