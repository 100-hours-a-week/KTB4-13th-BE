package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.PointRecoverySettlement;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRecoveryRepositoryAdapter implements PointRecoveryRepositoryPort {
    private final PointRecoveryJpaRepository recoveries;
    private final PointRecoverySettlementJpaRepository settlements;

    @Override
    public Optional<PointRecovery> findBySource(final PointType sourceType, final Long sourceReferenceId) {
        return recoveries.findBySourceTypeAndSourceReferenceId(sourceType, sourceReferenceId);
    }

    @Override
    public List<PointRecovery> findOutstandingByUserId(final Long userId) {
        return recoveries.findByUserIdAndRemainingAmountGreaterThanOrderByIdAsc(userId, BigDecimal.ZERO);
    }

    @Override
    public void save(final PointRecovery recovery) {
        recoveries.save(recovery);
    }

    @Override
    public void saveSettlement(final PointRecoverySettlement settlement) {
        settlements.save(settlement);
    }
}
