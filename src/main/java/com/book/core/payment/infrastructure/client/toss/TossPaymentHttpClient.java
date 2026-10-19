package com.book.core.payment.infrastructure.client.toss;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/v1/payments")
interface TossPaymentHttpClient {
    @PostExchange("/confirm")
    TossPaymentResponse approve(@RequestHeader("Idempotency-Key") final String idempotencyKey, @RequestBody final ApproveRequest request);

    @GetExchange("/{paymentKey}")
    TossPaymentResponse find(@PathVariable("paymentKey") final String paymentKey);

    @PostExchange("/{paymentKey}/cancel")
    TossPaymentResponse cancel(@PathVariable("paymentKey") final String paymentKey,
        @RequestHeader("Idempotency-Key") final String idempotencyKey, @RequestBody final CancelRequest request);

    record ApproveRequest(String paymentKey, String orderId, BigDecimal amount) {
        @Override
        public String toString() {
            return "ApproveRequest[redacted]";
        }
    }

    record CancelRequest(String cancelReason, BigDecimal cancelAmount, BigDecimal refundableAmount) {
    }
}
