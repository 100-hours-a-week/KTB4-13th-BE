package com.book.core.payment.application.port;

import com.book.core.payment.application.result.PgPaymentResult;
import java.math.BigDecimal;

public interface PaymentClient {
    PgPaymentResult approve(String paymentKey, String orderKey, BigDecimal amount, String idempotencyKey);

    PgPaymentResult find(String paymentKey);

    PgPaymentResult cancel(String paymentKey, BigDecimal amount, BigDecimal refundableAmount, String idempotencyKey);
}
