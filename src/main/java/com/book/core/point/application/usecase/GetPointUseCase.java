package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.result.PointHistoryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetPointUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;

    @Transactional(readOnly = true)
    public GetPointResult execute(final GetPointCommand command) {
        log.debug("[POINT] 포인트 조회를 시작합니다.");
        final var balance =
            pointBalanceRepository.findByUserId(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var histories =
            pointHistoryRepository.findByUserId(command.userId()).stream().map((final var history) -> new PointHistoryResult(history.id(),
                history.userId(), history.type(), history.referenceId(), history.amount(), history.createdAt())).toList();
        return new GetPointResult(balance.userId(), balance.available(), histories);
    }
}
