package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CancelRepositoryAdapter implements CancelRepositoryPort {
    private final CancelJpaRepository jpaRepository;

    @Override
    public Cancel save(final Cancel cancel) {
        return jpaRepository.save(cancel);
    }

    @Override
    public long countByOrderId(final Long orderId) {
        return jpaRepository.countByOrderId(orderId);
    }

    @Override
    public List<Cancel> findActiveByOrderIdLatestFirst(final Long orderId) {
        return jpaRepository.findByOrderIdAndDeletedAtIsNullOrderByCanceledAtDescIdDesc(orderId);
    }
}
