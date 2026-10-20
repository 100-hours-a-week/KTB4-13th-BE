package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointLotAllocationRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointLot;
import com.book.core.point.domain.PointLotAllocation;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RestorePaymentPointsUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointLotRepositoryPort pointLotRepository;
    private final PointLotAllocationRepositoryPort pointLotAllocationRepository;
    private final Clock clock;

    @Transactional
    public BigDecimal execute(final Long userId, final Long paymentId, final Long cancelId, final BigDecimal requestedAmount) {
        return execute(userId, paymentId, cancelId, requestedAmount, null);
    }

    @Transactional
    public BigDecimal execute(final Long userId, final Long paymentId, final Long cancelId, final BigDecimal requestedAmount,
        final Long orderItemId) {
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        final var previousRestorations =
            pointHistoryRepository.findByUserIdAndTypeAndReferenceId(userId, PointType.CANCEL_RETURN, cancelId);
        if (!previousRestorations.isEmpty()) {
            return previousRestorations.stream().map(PointHistory::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        final PointHistory deduction = pointHistoryRepository.findByUserIdAndTypeAndReferenceId(userId, PointType.PAYMENT, paymentId)
            .stream().filter((final PointHistory history) -> history.amount().signum() < 0).findFirst().orElse(null);
        if (deduction == null) {
            return BigDecimal.ZERO;
        }

        final LocalDateTime now = LocalDateTime.now(clock);
        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        BigDecimal remainingToRestore = requestedAmount.min(deduction.amount().abs());
        BigDecimal restoredAmount = BigDecimal.ZERO;
        final var allocations = pointLotAllocationRepository.findByDebitHistoryIdForUpdate(deduction.id()).stream()
            .filter((final PointLotAllocation allocation) -> orderItemId == null || orderItemId.equals(allocation.orderItemId())).toList();
        for (final PointLotAllocation allocation : allocations) {
            if (remainingToRestore.signum() == 0) {
                break;
            }
            if (allocation.restorableAmount().signum() == 0) {
                continue;
            }
            final PointLot pointLot = pointLotRepository.findByIdForUpdate(allocation.pointLotId()).orElse(null);
            if (pointLot == null || !pointLot.expiresAt().isAfter(now)) {
                continue;
            }
            final BigDecimal amount = allocation.restorableAmount().min(remainingToRestore);
            pointLot.restore(amount, now);
            allocation.restore(amount);
            restoredAmount = restoredAmount.add(amount);
            remainingToRestore = remainingToRestore.subtract(amount);
        }
        if (restoredAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }

        balance.apply(restoredAmount);
        pointHistoryRepository.save(PointHistory.builder().userId(userId).type(PointType.CANCEL_RETURN).referenceId(cancelId)
            .amount(restoredAmount).balanceAfter(balance.balance()).build());
        return restoredAmount;
    }
}
