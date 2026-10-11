package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointBalanceRepositoryAdapter implements PointBalanceRepositoryPort {

    private final PointBalanceJpaRepository jpaRepository;

    @Override
    public Optional<PointBalance> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }
}
