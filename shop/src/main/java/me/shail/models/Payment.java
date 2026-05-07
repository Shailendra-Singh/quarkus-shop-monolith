package me.shail.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.shail.models.base.AbstractEntity;
import me.shail.models.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "payments")
public class Payment extends AbstractEntity {
    @Column(name = "payment_id")
    public String paymentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public PaymentStatus status;

    @NotNull
    @Column(name = "amount", nullable = false)
    public BigDecimal amount;

    public Payment(String paymentId, @NotNull PaymentStatus status, @NotNull BigDecimal amount) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        Payment payment = (Payment) obj;
        return Objects.equals(paymentId, payment.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(paymentId);
    }
}
