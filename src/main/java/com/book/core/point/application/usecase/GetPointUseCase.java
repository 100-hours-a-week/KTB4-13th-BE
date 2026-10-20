package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.result.PointHistoryResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class GetPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final PointLotRepositoryPort pointLotRepository;
    private final Clock clock;

    public GetPointUseCase(final PointBalanceRepositoryPort pointBalanceRepository,
        final PointHistoryRepositoryPort pointHistoryRepository) {
        this(pointBalanceRepository, pointHistoryRepository, null, Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public GetPointResult execute(final GetPointCommand command) {
        log.debug("포인트 조회를 시작합니다. traceId={}", MDC.get("traceId"));
        final var balance =
            pointBalanceRepository.findByUserId(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var histories =
            pointHistoryRepository.findByUserId(command.userId()).stream().map((final var history) -> new PointHistoryResult(history.id(),
                history.userId(), history.type(), history.referenceId(), history.amount(), history.createdAt())).toList();
        final BigDecimal availableBalance;
        if (balance.balance().signum() < 0) {
            availableBalance = balance.balance();
        } else {
            availableBalance = pointLotRepository == null ? balance.balance()
                : pointLotRepository.sumAvailableByUserId(command.userId(), LocalDateTime.now(clock));
        }
        return new GetPointResult(balance.userId(), availableBalance, histories);
    }
}
