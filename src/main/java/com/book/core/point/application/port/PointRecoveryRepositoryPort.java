package com.book.core.point.application.port;

import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.PointRecoverySettlement;
import com.book.core.point.domain.enums.PointType;
import java.util.List;
import java.util.Optional;

public interface PointRecoveryRepositoryPort {
    Optional<PointRecovery> findBySource(PointType sourceType, Long sourceReferenceId);

    List<PointRecovery> findOutstandingByUserId(Long userId);

    void save(PointRecovery recovery);

    void saveSettlement(PointRecoverySettlement settlement);
}
