package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CancelBalanceRepositoryAdapter implements CancelBalanceRepositoryPort {
    private final CancelBalanceJpaRepository jpaRepository;

    @Override
    public Optional<CancelBalance> findByOrderId(final Long orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public CancelBalance save(final CancelBalance entity) {
        return jpaRepository.save(entity);
    }
}
