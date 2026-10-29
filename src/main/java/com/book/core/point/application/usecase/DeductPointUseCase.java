package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class DeductPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointRecoveryRepositoryPort recoveries;

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 금액이 0이어서 포인트 차감을 생략합니다. type={}, referenceId={}", type,
                referenceId);
            return;
        }
        log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 차감 처리를 시작합니다. type={}, referenceId={}", type, referenceId);

        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (type == PointType.REVIEW && amount.signum() > 0) {
            if (referenceId == null) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            final var existing = recoveries.findBySource(type, referenceId);
            if (existing.isPresent()) {
                if (!existing.get().userId().equals(userId) || existing.get().originalAmount().compareTo(amount) != 0) {
                    throw new CoreException(ErrorCode.INVALID_REQUEST);
                }
                return;
            }
            final BigDecimal collected = amount.min(balance.balance().max(BigDecimal.ZERO));
            balance.apply(collected.negate());
            recoveries.save(new PointRecovery(userId, type, referenceId, amount, amount.subtract(collected)));
            if (collected.signum() > 0) {
                pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId)
                    .amount(collected.negate()).balanceAfter(balance.balance()).build());
            }
        } else {
            final BigDecimal deductedAmount = amount.negate();
            balance.apply(deductedAmount);
            pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(deductedAmount)
                .balanceAfter(balance.balance()).build());
        }
        log.atInfo().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 차감 처리를 마쳤습니다. type={}, referenceId={}", type, referenceId);
    }
}
