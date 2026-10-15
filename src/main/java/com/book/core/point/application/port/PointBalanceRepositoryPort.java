package com.book.core.point.application.port;

import com.book.core.point.domain.PointBalance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointBalanceRepositoryPort {
    Optional<PointBalance> findByUserId(final Long userId);

    void save(final PointBalance balance);

    boolean existsByUserId(final Long userId);

    List<Long> findActiveUserIdsWithoutPointBalance(final LocalDateTime registeredBefore, final Long afterUserId, final int limit);
}
