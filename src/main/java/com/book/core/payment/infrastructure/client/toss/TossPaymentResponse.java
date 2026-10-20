package com.book.core.payment.infrastructure.client.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record TossPaymentResponse(String paymentKey, String orderId, BigDecimal totalAmount, BigDecimal balanceAmount, String status,
    String method, Card card, List<Cancel> cancels) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Card(String approveNo) {
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Cancel(String transactionKey, BigDecimal cancelAmount, BigDecimal refundableAmount, String cancelStatus,
        OffsetDateTime canceledAt) {
    }

    @Override
    public String toString() {
        return "TossPaymentResponse[redacted]";
    }
}
