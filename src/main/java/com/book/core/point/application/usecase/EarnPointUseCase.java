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
import com.book.core.point.domain.PointRecoverySettlement;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EarnPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointRecoveryRepositoryPort recoveries;

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 금액이 0이어서 포인트 적립을 생략합니다. type={}, referenceId={}", type,
                referenceId);
            return;
        }

        log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 적립 처리를 시작합니다. type={}, referenceId={}", type, referenceId);

        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final BigDecimal balanceBefore = balance.balance();
        balance.apply(amount);
        pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount)
            .balanceAfter(balance.balance()).build());
        if (amount.signum() > 0) {
            // 기존 DECIMAL(10,0) 컬럼의 저장 단위를 기준으로 복원액도 상계한다.
            final BigDecimal storedBefore = balanceBefore.setScale(0, RoundingMode.HALF_UP);
            final BigDecimal storedAfter = balance.balance().setScale(0, RoundingMode.HALF_UP);
            BigDecimal remaining = storedAfter.subtract(storedBefore).max(BigDecimal.ZERO).min(storedAfter.max(BigDecimal.ZERO));
            final var outstanding = recoveries.findOutstandingByUserId(userId);
            if (remaining.signum() > 0 && !outstanding.isEmpty()) {
                // 상계가 실제 DB에 반영될 정수 잔액에서 진행되도록 우선 정규화한다.
                balance.apply(storedAfter.subtract(balance.balance()));
            }
            for (final PointRecovery recovery : outstanding) {
                if (remaining.signum() == 0) {
                    break;
                }
                final BigDecimal settled = recovery.settle(remaining);
                remaining = remaining.subtract(settled);
                balance.apply(settled.negate());
                String sourceType = null;
                if (type != null) {
                    sourceType = type.name();
                }
                recoveries.saveSettlement(new PointRecoverySettlement(recovery.id(), sourceType, referenceId, settled));
                pointHistoryRepository.save(PointHistory.builder().userId(userId).type(recovery.sourceType())
                    .referenceId(recovery.sourceReferenceId()).amount(settled.negate()).balanceAfter(balance.balance()).build());
            }
        }
        log.atInfo().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 적립 처리를 마쳤습니다. type={}, referenceId={}", type, referenceId);
    }
}
