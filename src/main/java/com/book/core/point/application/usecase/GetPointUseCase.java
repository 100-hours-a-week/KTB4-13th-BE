package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.result.PointHistoryResult;
import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;

    @Transactional(readOnly = true)
    public GetPointResult execute(final GetPointCommand command) {
        log.debug("포인트 조회를 시작합니다. traceId={}", MDC.get("traceId"));
        final BigDecimal balance = pointBalanceRepository.findByUserId(command.userId()).map(PointBalance::balance).orElse(BigDecimal.ZERO);
        final var histories =
            pointHistoryRepository.findByUserId(command.userId()).stream().map((final var history) -> new PointHistoryResult(history.id(),
                history.userId(), history.type(), history.referenceId(), history.amount(), history.createdAt())).toList();
        return new GetPointResult(command.userId(), balance, histories);
    }
}
