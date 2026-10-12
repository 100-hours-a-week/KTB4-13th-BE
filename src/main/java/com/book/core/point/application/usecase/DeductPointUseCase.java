package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.command.DeductPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointHistory;
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
    public void execute(final DeductPointCommand command) {
        if (command.amount().compareTo(BigDecimal.ZERO) == 0) {
            log.debug("금액이 0이어서 포인트 차감을 생략합니다. type={}, referenceId={}, traceId={}", command.type(), command.referenceId(),
                MDC.get("traceId"));
            return;
        }
        log.debug("포인트 차감 처리를 시작합니다. type={}, referenceId={}, traceId={}", command.type(), command.referenceId(), MDC.get("traceId"));
        // NOTE: 모든 유저는 가입 시 Point 테이블 생성
        final var balance =
            pointBalanceRepository.findByUserId(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final BigDecimal amount = command.amount().negate();
        balance.apply(amount);
        pointHistoryRepository.save(PointHistory.builder().userId(command.userId()).type(command.type()).referenceId(command.referenceId())
            .amount(amount).balanceAfter(balance.balance()).build());
        log.info("포인트 차감 처리를 마쳤습니다. type={}, referenceId={}, traceId={}", command.type(), command.referenceId(), MDC.get("traceId"));
    }
}
