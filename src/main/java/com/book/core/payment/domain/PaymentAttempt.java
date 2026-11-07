package com.book.core.payment.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "payment_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PaymentAttempt extends BaseEntity {
    @Column(nullable = false)
    private Long paymentId;
    @Column(nullable = false, length = 255)
    private String orderKey;
    @Column(nullable = false, unique = true, length = 64)
    private String pgOrderId;
    @Column(nullable = false, unique = true, length = 64)
    private String requestHash;
    @Column(nullable = false, length = 64)
    private String inputHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentAttemptState state;

    public PaymentAttempt(final Long paymentId, final String orderKey, final String pgOrderId, final String requestHash,
        final String inputHash) {
        if (paymentId == null || orderKey == null || orderKey.isBlank() || pgOrderId == null || pgOrderId.isBlank() || requestHash == null
            || requestHash.isBlank() || inputHash == null || inputHash.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.paymentId = paymentId;
        this.orderKey = orderKey;
        this.pgOrderId = pgOrderId;
        this.requestHash = requestHash;
        this.inputHash = inputHash;
        this.state = PaymentAttemptState.READY;
    }

    public void fail() {
        if (state == PaymentAttemptState.COMPLETED) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        state = PaymentAttemptState.FAILED;
    }

    public void complete() {
        if (state != PaymentAttemptState.READY) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        state = PaymentAttemptState.COMPLETED;
    }
}
