package com.book.core.payment.application.result;

import java.time.OffsetDateTime;

public record CanceledPaymentResult(String transactionKey, OffsetDateTime canceledAt) {
    @Override
    public String toString() {
        return "CanceledPaymentResult[redacted]";
    }
}
