package com.book.core.point.application.port;

import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;

import java.util.Optional;

public interface PointBalanceRepositoryPort {
    Optional<PointBalance> findByUserId(final Long userId);

    Optional<PointBalance> findByUserIdForUpdate(final Long userId);

    default BigDecimal availableBalance(final PointBalance balance) {
        return balance.balance();
    }

    void save(final PointBalance balance);
}
