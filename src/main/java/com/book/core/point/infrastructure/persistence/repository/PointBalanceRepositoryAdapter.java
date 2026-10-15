package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointBalanceRepositoryAdapter implements PointBalanceRepositoryPort {

    private final PointBalanceJpaRepository jpaRepository;

    @Override
    public Optional<PointBalance> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public void save(final PointBalance balance) {
        jpaRepository.save(balance);
    }

    @Override
    public boolean existsByUserId(final Long userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public List<Long> findActiveUserIdsWithoutPointBalance(final LocalDateTime registeredBefore, final Long afterUserId, final int limit) {
        return jpaRepository.findActiveUserIdsWithoutPointBalance(registeredBefore, afterUserId, limit);
    }
}
