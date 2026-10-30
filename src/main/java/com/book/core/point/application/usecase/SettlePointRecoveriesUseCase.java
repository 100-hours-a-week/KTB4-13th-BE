package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.PointRecoverySettlement;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SettlePointRecoveriesUseCase {
    private final PointRecoveryRepositoryPort recoveries;
    private final PointHistoryRepositoryPort histories;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final PointBalance balance, final BigDecimal amount, final String sourceType, final Long sourceReferenceId) {
        BigDecimal remaining = amount.min(balance.available());
        for (final PointRecovery recovery : recoveries.findOutstandingByUserId(balance.userId())) {
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
}
