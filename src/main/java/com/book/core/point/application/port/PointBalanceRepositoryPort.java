package com.book.core.point.application.port;

import com.book.core.point.domain.PointBalance;

import java.util.Optional;

public interface PointBalanceRepositoryPort {
    Optional<PointBalance> findByUserId(final Long userId);

    default Optional<PointBalance> findByUserIdForUpdate(final Long userId) {
        return findByUserId(userId);
    }

    void save(final PointBalance balance);
}
