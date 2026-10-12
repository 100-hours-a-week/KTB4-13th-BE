package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointHistory;
import java.util.List;
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
    public void save(final PointHistory history) {
        jpaRepository.save(history);
    }
}
