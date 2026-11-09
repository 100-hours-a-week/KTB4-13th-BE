package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.domain.PaymentAttempt;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PaymentAttemptRepositoryAdapter implements PaymentAttemptRepositoryPort {
    private final PaymentAttemptJpaRepository repository;

    @Override
    public PaymentAttempt save(final PaymentAttempt attempt) {
        return repository.save(attempt);
    }

    @Override
    public Optional<PaymentAttempt> findById(final Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<PaymentAttempt> findByPgOrderId(final String pgOrderId) {
        return repository.findByPgOrderId(pgOrderId);
    }

    @Override
    public Optional<PaymentAttempt> findByRequestHash(final String requestHash) {
        return repository.findByRequestHash(requestHash);
    }

    @Override
    public Optional<PaymentAttempt> findLatestByPaymentId(final Long paymentId) {
        return repository.findFirstByPaymentIdOrderByIdDesc(paymentId);
    }
}
