package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointLot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointLotRepositoryAdapter implements PointLotRepositoryPort {
    private final PointLotJpaRepository jpaRepository;

    @Override
    public PointLot save(final PointLot pointLot) {
        return jpaRepository.save(pointLot);
    }

    @Override
    public Optional<PointLot> findByIdForUpdate(final Long pointLotId) {
        return jpaRepository.findByIdForUpdate(pointLotId);
    }

    @Override
    public List<PointLot> findAvailableByUserIdForUpdate(final Long userId, final LocalDateTime now) {
        return jpaRepository.findAvailableByUserIdForUpdate(userId, now);
    }

    @Override
    public List<PointLot> findExpiredForUpdate(final LocalDateTime now, final int limit) {
        return jpaRepository.findExpiredForUpdate(now, PageRequest.of(0, limit));
    }

    @Override
    public List<PointLot> findExpiringForUpdate(final LocalDateTime now, final LocalDateTime expiresBefore, final int limit) {
        return jpaRepository.findExpiringForUpdate(now, expiresBefore, PageRequest.of(0, limit));
    }

    @Override
    public BigDecimal sumAvailableByUserId(final Long userId, final LocalDateTime now) {
        return jpaRepository.sumAvailableByUserId(userId, now);
    }
}
