package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
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
public class DeductPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;

    @Transactional
    public void execute(final Long userId, final PointType type, final Long referenceId, final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("금액이 0이어서 포인트 차감을 생략합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
            return;
        }
        log.debug("포인트 차감 처리를 시작합니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
        // NOTE: 모든 유저는 가입 시 Point 테이블 생성
        final var balance = pointBalanceRepository.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final BigDecimal deductedAmount = amount.negate();
        balance.apply(deductedAmount);
        pointHistoryRepository.save(PointHistory.builder().userId(userId).type(type).referenceId(referenceId).amount(deductedAmount)
            .balanceAfter(balance.balance()).build());
        log.info("포인트 차감 처리를 마쳤습니다. type={}, referenceId={}, traceId={}", type, referenceId, MDC.get("traceId"));
    }
}
