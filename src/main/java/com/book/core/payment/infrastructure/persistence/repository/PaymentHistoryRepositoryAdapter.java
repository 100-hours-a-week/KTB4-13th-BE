package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PaymentHistoryRepositoryAdapter implements PaymentHistoryRepositoryPort {
    private final PaymentHistoryJpaRepository jpaRepository;

    @Override
    public PaymentHistory save(final PaymentHistory entity) {
        return jpaRepository.save(entity);
    }
}
