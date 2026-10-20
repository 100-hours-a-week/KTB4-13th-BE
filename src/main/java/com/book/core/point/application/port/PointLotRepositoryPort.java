package com.book.core.point.application.port;

import com.book.core.point.domain.PointLot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointLotRepositoryPort {
    PointLot save(final PointLot pointLot);

    Optional<PointLot> findByIdForUpdate(final Long pointLotId);

    List<PointLot> findAvailableByUserIdForUpdate(final Long userId, final LocalDateTime now);

    List<PointLot> findExpiredForUpdate(final LocalDateTime now, final int limit);

    List<PointLot> findExpiringForUpdate(final LocalDateTime now, final LocalDateTime expiresBefore, final int limit);

    BigDecimal sumAvailableByUserId(final Long userId, final LocalDateTime now);
}
