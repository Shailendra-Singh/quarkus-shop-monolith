package me.shail.repositories;


import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import me.shail.dtos.PaymentDto;
import me.shail.models.Payment;
import org.hibernate.annotations.processing.HQL;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends PanacheRepository.Reactive.Stateless<Payment, UUID> {

    @HQL("where amount between :amountMin and :amountMax")
    Uni<List<Payment>> findByAmountBetween(BigDecimal amountMin, BigDecimal amountMax);

    @HQL("select p.id, p.paymentReferenceId, p.status, o.id from Order o join o.payment p where p.amount <= :amountMax")
    default Uni<List<PaymentDto>> findPaymentDtoByMaxPrice(BigDecimal amountMax) {
        return find("select p.id, p.paymentReferenceId, p.status, o.id" +
                " from Order o join o.payment p" +
                " where p.amount <= :amountMax", amountMax)
                .project(PaymentDto.class).list();
    }
}
