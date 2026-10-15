package com.book.core.payment.application.port;

import com.book.core.payment.domain.PaymentHistory;

public interface PaymentHistoryRepositoryPort {
    PaymentHistory save(final PaymentHistory entity);
}
