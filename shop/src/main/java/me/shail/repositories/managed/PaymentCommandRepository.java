package me.shail.repositories.managed;

import io.quarkus.hibernate.panache.PanacheRepository;
import me.shail.models.Payment;

import java.util.UUID;

public interface PaymentCommandRepository extends PanacheRepository.Reactive<Payment, UUID> {
}
