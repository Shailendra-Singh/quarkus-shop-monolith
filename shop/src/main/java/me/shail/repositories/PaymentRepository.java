package me.shail.repositories;


import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.shail.models.Payment;
import me.shail.repositories.managed.PaymentCommandRepository;
import me.shail.repositories.stateless.PaymentQueryRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PaymentRepository {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    PaymentCommandRepository paymentCommandRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    PaymentQueryRepository paymentQueryRepository;

    public Uni<Payment> create(Payment payment) {
        return paymentCommandRepository.persist(payment).replaceWith(payment);
    }

    public Uni<Boolean> delete(UUID paymentId) {
        return this.paymentCommandRepository.deleteById(paymentId);
    }

    public Uni<Payment> findByIdManaged(UUID paymentId) {
        return paymentCommandRepository.findById(paymentId);
    }

    public Uni<Payment> findByIdStateless(UUID paymentId) {
        return paymentQueryRepository.findById(paymentId);
    }

    public Uni<List<Payment>> findPaymentDtoByMaxPrice(BigDecimal maxAmount) {
        return paymentQueryRepository.findPaymentDtoByMaxPrice(maxAmount);
    }
}
