package me.shail.repositories;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import me.shail.models.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends PanacheRepositoryBase<Payment, UUID> {
    default Uni<List<Payment>> findByAmountBetween(BigDecimal amountMin, BigDecimal amountMax) {
        return list("amount BETWEEN ?1 AND ?2", amountMin, amountMax);
    }
}
