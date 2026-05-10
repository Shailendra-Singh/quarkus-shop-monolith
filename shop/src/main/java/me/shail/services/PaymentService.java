package me.shail.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.shail.dtos.PaymentDto;
import me.shail.models.Order;
import me.shail.models.Payment;
import me.shail.models.enums.OrderStatus;
import me.shail.models.enums.PaymentStatus;
import me.shail.repositories.OrderRepository;
import me.shail.repositories.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Transactional
@ApplicationScoped
public class PaymentService {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    PaymentRepository paymentRepository;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    OrderRepository orderRepository;

    public static PaymentDto mapToDto(Payment payment, UUID orderId) {
        if (payment != null) {
            return new PaymentDto(payment.id, payment.paymentReferenceId, payment.status.name(), orderId);
        }
        return null;
    }

    public Uni<List<PaymentDto>> findByPriceRange(Double max) {
        return this.paymentRepository.findPaymentDtoByMaxPrice(BigDecimal.valueOf(max));
    }

    public Uni<PaymentDto> findById(UUID id) {
        log.debug("Request to get Payment: {}", id);
        return this.orderRepository.findByPaymentId(id)
                .onItem().ifNull().failWith(
                        () -> new IllegalStateException("The order doesn't exist. Id: " + id)
                )
                .chain(order ->
                        this.paymentRepository.findById(order.payment.id)
                                .onItem().ifNotNull().transform(p -> new PaymentDto(id,
                                        p.paymentReferenceId,
                                        p.status.name(),
                                        order.id))
                                .onItem().ifNull().continueWith((PaymentDto) null)
                );
    }

    public Uni<PaymentDto> create(PaymentDto paymentDto) {
        log.debug("Request to create Payment: {}", paymentDto);
        return this.orderRepository.findByPaymentId(paymentDto.orderId())
                .onItem().ifNull().failWith(
                        () -> new IllegalStateException("The order doesn't exist. Id: " + paymentDto.orderId())
                )
                .chain(order -> {
                    order.status = OrderStatus.PAID;
                    Payment payment = new Payment(
                            paymentDto.paymentReferenceId(),
                            PaymentStatus.valueOf(paymentDto.status()),
                            order.price
                    );
                    return Uni.combine().all().unis(
                            this.orderRepository.update(order),
                            this.paymentRepository.insert(payment)
                    ).asTuple().onItem().transform(Tuple2::getItem1);
                }).replaceWith(paymentDto);
    }

    public Uni<Order> findOrderByPaymentId(UUID id) {
        return this.orderRepository.findByPaymentId(id)
                .onItem().ifNull().failWith(() ->
                        new IllegalStateException("The order doesn't exist. Id: " + id)
                );
    }

    public Uni<Boolean> delete(UUID id) {
        log.debug("Request to delete Payment: {}", id);
        return this.paymentRepository.deleteById(id);
    }
}
