package me.shail.repositories.stateless;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.models.Payment;
import org.hibernate.annotations.processing.HQL;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentQueryRepository extends PanacheRepository.Reactive.Stateless<Payment, UUID> {
    /**
     * Finds payments in a given amount range
     *
     * @param amountMin minimum amount(inclusive)
     * @param amountMax maximum amount (inclusive)
     * @return list of payments
     */
    @HQL("where amount between :amountMin and :amountMax")
    Uni<List<Payment>> findByAmountBetween(BigDecimal amountMin, BigDecimal amountMax);

    /**
     * Finds all payments which are less than a given amount
     *
     * @param amountMax max amount a payment can have (inclusive)
     * @return list of payments - project to PaymentDto
     */
    @HQL("where amount <= :amountMax")
    Uni<List<Payment>> findPaymentDtoByMaxPrice(BigDecimal amountMax);
}
