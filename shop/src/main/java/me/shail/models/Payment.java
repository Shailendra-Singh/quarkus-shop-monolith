package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.PaymentStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "payments")
public class Payment extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    public Order order;

    @Column(name = "payment_reference_id", nullable = false, unique = true, updatable = false)
    public String paymentReferenceId;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    public Instant createdAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public PaymentStatus status;

    @NotNull
    @Column(name = "amount", nullable = false)
    public BigDecimal amount;

    public Payment(@NotNull Order order, @NotNull PaymentStatus status, @NotNull BigDecimal amount) {
        this.order = order;
        this.status = status;
        this.amount = amount;
        this.paymentReferenceId = UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Payment payment = (Payment) obj;
        return Objects.equals(paymentReferenceId, payment.paymentReferenceId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
