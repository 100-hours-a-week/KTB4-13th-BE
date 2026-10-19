package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EarnPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("금액이 0이어서 포인트 적립을 생략합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
            return;
        }

        log.debug("포인트 적립 처리를 시작합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));

        final PointBalance balance = pointBalanceRepository.findByUserId(userId).orElseGet(() -> createBalance(userId));
        balance.apply(amount);
        pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(amount)
            .balanceAfter(balance.balance()).build());
        log.info("포인트 적립 처리를 마쳤습니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
    }

    // 최초 적립이 동시에 들어와도 UNIQUE(user_id)로 한 행만 만들고, 잠금 조회로 먼저 커밋된 적립까지 반영한 잔액에 더합니다.
    private PointBalance createBalance(final Long userId) {
        pointBalanceRepository.createIfAbsent(userId);
        return pointBalanceRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
    }
}
