package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryAdapter implements PointHistoryRepositoryPort {
    private final PointHistoryJpaRepository jpaRepository;

    @Override
    public List<PointHistory> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<PointHistory> findByUserIdAndTypeAndReferenceId(final Long userId, final PointType type, final Long referenceId) {
        return jpaRepository.findByUserIdAndTypeAndReferenceId(userId, type, referenceId);
    }

    @Override
    public Optional<PointHistory> findByUserIdAndEventKey(final Long userId, final String eventKey) {
        return jpaRepository.findByUserIdAndEventKeyAndDeletedAtIsNull(userId, eventKey);
    }

    @Override
    public PointHistory save(final PointHistory history) {
        return jpaRepository.save(history);
    }
}
