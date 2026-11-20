package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.application.port.PointRecoverySettlementPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.PointRecoverySettlement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SettlePointRecoveriesUseCase implements PointRecoverySettlementPort {
    private final PointRecoveryRepositoryPort recoveries;
    private final PointHistoryRepositoryPort histories;

    public void execute(final PointBalance balance, final BigDecimal availableAmount, final String sourceType,
        final Long sourceReferenceId) {
        if (availableAmount.signum() <= 0) {
            return;
        }
        final var outstanding = recoveries.findOutstandingByUserId(balance.userId());
        if (outstanding.isEmpty()) {
            return;
        }

        final BigDecimal storedBalance = balance.balance().setScale(0, RoundingMode.HALF_UP);
        balance.apply(storedBalance.subtract(balance.balance()));
        BigDecimal remaining = availableAmount.min(storedBalance.max(BigDecimal.ZERO));
        for (final PointRecovery recovery : outstanding) {
            if (remaining.signum() == 0) {
                break;
            }
            final BigDecimal settled = recovery.settle(remaining);
            remaining = remaining.subtract(settled);
            balance.apply(settled.negate());
            recoveries.saveSettlement(new PointRecoverySettlement(recovery.id(), sourceType, sourceReferenceId, settled));
            histories.save(PointHistory.builder().userId(balance.userId()).type(recovery.sourceType())
                .referenceId(recovery.sourceReferenceId()).amount(settled.negate()).balanceAfter(balance.balance()).build());
        }
    }

    @Override
    public void settleReleasedReservation(final PointBalance balance, final BigDecimal releasedAmount, final Long attemptId) {
        execute(balance, releasedAmount, "RELEASE", attemptId);
    }
}
