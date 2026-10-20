package com.book.core.payment.application.result;

public record PaymentCompletionResult(Long paymentId, boolean completedNow) {
}
