package com.book.core.payment.infrastructure.client.toss;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "tossPayments", url = "${payment.toss.base-url}", configuration = TossPaymentFeignConfiguration.class)
interface TossPaymentFeignClient {
    @PostMapping("/v1/payments/confirm")
    TossPaymentResponse approve(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody ApproveRequest request);

    @GetMapping("/v1/payments/{paymentKey}")
    TossPaymentResponse find(@PathVariable("paymentKey") String paymentKey);

    @PostMapping("/v1/payments/{paymentKey}/cancel")
    TossPaymentResponse cancel(@PathVariable("paymentKey") String paymentKey, @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody CancelRequest request);

    record ApproveRequest(String paymentKey, String orderId, BigDecimal amount) {
        @Override
        public String toString() {
            return "ApproveRequest[redacted]";
        }
    }
    record CancelRequest(String cancelReason, BigDecimal cancelAmount, BigDecimal refundableAmount) {
    }
}
