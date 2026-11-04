package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReclaimPaymentBonusUseCase {
    private final PointBalanceRepositoryPort balances;
    private final PointRecoveryRepositoryPort recoveries;
    private final PointHistoryRepositoryPort histories;

    @Transactional
    public void execute(final Long userId, final Long paymentId, final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        if (userId == null || paymentId == null || amount == null || amount.signum() < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (amount.signum() == 0) {
            return;
        }
        final PointBalance balance = balances.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var existing = recoveries.findBySource(PointType.PAYMENT, paymentId);
        if (existing.isPresent()) {
            if (!existing.get().userId().equals(userId) || existing.get().originalAmount().compareTo(amount) != 0) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            return;
        }
        final BigDecimal collected = amount.min(balance.available());
        balance.apply(collected.negate());
        recoveries.save(new PointRecovery(userId, PointType.PAYMENT, paymentId, amount, amount.subtract(collected)));
        if (collected.signum() > 0) {
            histories.save(PointHistory.builder().userId(userId).type(PointType.PAYMENT).referenceId(paymentId).amount(collected.negate())
                .balanceAfter(balance.balance()).build());
        }
    }
}
