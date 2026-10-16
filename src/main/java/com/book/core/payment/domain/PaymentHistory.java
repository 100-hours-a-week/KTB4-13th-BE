package com.book.core.payment.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.payment.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "transaction_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PaymentHistory extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TransactionType type;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "external_payment_key", nullable = false)
    private String externalPaymentKey;
    @Column(name = "paid_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal paidAmount;
    @Column(name = "point_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal pointAmount;
    @Column(name = "coupon_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal couponAmount;
    @Column(name = "message", nullable = false)
    private String message;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public PaymentHistory(final TransactionType type, final Long userId, final Long orderId, final Long paymentId,
        final String externalPaymentKey, final BigDecimal paidAmount, final BigDecimal pointAmount, final BigDecimal couponAmount,
        final String message, final LocalDateTime occurredAt) {
        this.type = type;
        this.userId = userId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.externalPaymentKey = externalPaymentKey;
        this.paidAmount = paidAmount;
        this.pointAmount = pointAmount;
        this.couponAmount = couponAmount;
        this.message = message;
        this.occurredAt = occurredAt;
    }
}
