package com.book.core.payment.application.port;

import com.book.core.payment.domain.PaymentAttempt;
import java.util.Optional;

public interface PaymentAttemptRepositoryPort {
    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt> findById(Long id);

    Optional<PaymentAttempt> findByPgOrderId(String pgOrderId);

    Optional<PaymentAttempt> findByRequestHash(String requestHash);

    Optional<PaymentAttempt> findLatestByPaymentId(Long paymentId);
}
