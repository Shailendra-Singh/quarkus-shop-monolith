package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import me.shail.models.Category;

import java.util.UUID;

public interface CategoryCommandRepository extends PanacheRepository.Reactive<Category, UUID> {
}
