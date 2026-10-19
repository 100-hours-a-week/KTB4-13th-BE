package com.book.core.payment.application.result;

import com.book.core.payment.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PgPaymentResult(String paymentKey, String orderKey, BigDecimal totalAmount, BigDecimal balanceAmount, String status,
    PaymentMethod method, String approveCode, List<Cancellation> cancellations) {
    public PgPaymentResult {
        cancellations = List.copyOf(cancellations);
    }

    public record Cancellation(String transactionKey, BigDecimal amount, BigDecimal refundableAmount, String status,
        OffsetDateTime canceledAt) {
    }

    @Override
    public String toString() {
        return "PgPaymentResult[redacted]";
    }
}
