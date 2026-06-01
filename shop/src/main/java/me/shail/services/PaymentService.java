package me.shail.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.PaymentDto;
import me.shail.interceptors.WithCustomStatelessSession;
import me.shail.models.Payment;
import me.shail.models.enums.PaymentStatus;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.PaymentRepository;

import javax.naming.OperationNotSupportedException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class PaymentService {

    public final static String PAYMENT_NOT_EXIST_ERROR_MSG = "Payment does not exist. ID: ";

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    OrderRepository orderRepository;

    public static PaymentDto mapToDto(Payment payment) {
        if (payment != null) {
            return new PaymentDto(payment.id, payment.paymentReferenceId, payment.status.name(), payment.order.id);
        }
        return null;
    }

    @WithCustomStatelessSession
    public Uni<List<PaymentDto>> findByPriceRange(BigDecimal max) {
        return this.paymentRepository.findPaymentDtoByMaxPrice(max)
                .onItem().transformToUni(payments -> {
                    List<PaymentDto> paymentDtoList = payments.stream().map(PaymentService::mapToDto).toList();
                    return Uni.createFrom().item(paymentDtoList);
                });
    }

    @WithCustomStatelessSession
    public Uni<PaymentDto> findById(UUID paymentId) {
        log.debug("Request to get Payment: {}", paymentId);
        return generateUni_FindById(this.paymentRepository, paymentId, false)
                .onItem().transform(PaymentService::mapToDto);
    }

    @WithTransaction
    public Uni<PaymentDto> create(UUID orderId) {
        log.debug("Request to create Payment for order ID: {}", orderId);

        return OrderService.generateUni_FindById(this.orderRepository, orderId, true)
                .chain(order -> {
                    // The Domain Model handles the business rules
                    if (!order.canAcceptNewPayment()) {
                        Payment conflictingPayment = order.getConflictivePayment();
                        return Uni.createFrom().failure(new OperationNotSupportedException(
                                String.format("Cannot pay order %s. Existing payment is %s",
                                        orderId,
                                        conflictingPayment.status
                                )
                        ));
                    }
                    // Future: call another service (say TransactionService) to manage payment status
                    // If valid, statefully transition or just instantiate
                    Payment payment = new Payment(order, PaymentStatus.PENDING, order.price);

                    return this.paymentRepository.create(payment);
                })
                .onItem().transform(PaymentService::mapToDto);
    }

    @WithTransaction
    public Uni<Boolean> generateRefund(UUID paymentId) {
        log.debug("Request to generate refund for payment: {}", paymentId);
        // Future: call another service (say TransactionService) to manage refunds
        return generateUni_FindById(this.paymentRepository, paymentId, true)
                .chain(_ -> Uni.createFrom().item(Boolean.TRUE));
    }

    public static Uni<Payment> generateUni_FindById(PaymentRepository repository,
                                                    UUID paymentId,
                                                    boolean managed) {
        Uni<Payment> generatedUni;
        if (managed)
            generatedUni = repository.findByIdManaged(paymentId);
        else
            generatedUni = repository.findByIdStateless(paymentId);
        return generatedUni
                .onItem()
                .ifNull().failWith(() -> new EntityNotFoundException(getPaymentNotExistErrorMsg(paymentId)));
    }

    public static String getPaymentNotExistErrorMsg(UUID paymentId) {
        return PAYMENT_NOT_EXIST_ERROR_MSG + paymentId;
    }
}
