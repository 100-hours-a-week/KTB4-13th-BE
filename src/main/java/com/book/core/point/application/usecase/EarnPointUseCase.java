package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointEarnEventRepositoryPort;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointEarnEvent;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EarnPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final SettlePointRecoveriesUseCase settlePointRecoveries;
    private final PointEarnEventRepositoryPort earnEvents;

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        PointAmount.requireWhole(amount);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 금액이 0이어서 포인트 적립을 생략합니다. type={}, referenceId={}", type,
                referenceId);
            return;
        }

        log.atDebug().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 적립 처리를 시작합니다. type={}, referenceId={}", type, referenceId);

        final PointBalance balance =
            pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (amount.signum() > 0) {
            final var existing = earnEvents.find(userId, type, referenceId);
            if (existing.isPresent()) {
                if (existing.get().amount().compareTo(amount) != 0) {
                    throw new CoreException(ErrorCode.INVALID_REQUEST);
                }
                return;
            }
            final PointEarnEvent event = new PointEarnEvent(userId, type, referenceId, amount);
            earnEvents.save(event);
            balance.apply(amount);
            pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount)
                .balanceAfter(balance.balance()).build());
            settlePointRecoveries.execute(balance, amount, "EARN", event.id());
        } else {
            if (balance.reserved().signum() > 0 && amount.negate().compareTo(balance.available()) > 0) {
                throw new CoreException(ErrorCode.POINT_EXCEEDS_BALANCE);
            }
            balance.apply(amount);
            pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount)
                .balanceAfter(balance.balance()).build());
        }
        log.atInfo().addKeyValue("referenceId", referenceId).log("[POINT] 포인트 적립 처리를 마쳤습니다. type={}, referenceId={}", type, referenceId);
    }
}
