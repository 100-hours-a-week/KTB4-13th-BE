package com.book.core.payment.application.result;

import com.book.core.payment.domain.enums.PaymentMethod;

public record ApprovedPaymentResult(String paymentKey, PaymentMethod method, String approveCode) {
    @Override
    public String toString() {
        return "ApprovedPaymentResult[redacted]";
    }
}
