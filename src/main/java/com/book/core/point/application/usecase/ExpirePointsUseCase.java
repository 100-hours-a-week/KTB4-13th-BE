package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointLot;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExpirePointsUseCase {
    private static final int BATCH_SIZE = 500;

    private final PointLotRepositoryPort pointLotRepository;
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointHistoryRepositoryPort pointHistoryRepository;
    private final Clock clock;

    @Transactional
    public int execute() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final var expiredLots = pointLotRepository.findExpiredForUpdate(now, BATCH_SIZE);
        for (final PointLot pointLot : expiredLots) {
            final BigDecimal expiredAmount = pointLot.expire(now);
            if (expiredAmount.signum() == 0) {
                continue;
            }
            final PointBalance balance = pointBalanceRepository.findByUserIdForUpdate(pointLot.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
            balance.apply(expiredAmount.negate());
            pointHistoryRepository.save(PointHistory.builder().userId(pointLot.userId()).type(PointType.EXPIRATION)
                .referenceId(pointLot.id()).amount(expiredAmount.negate()).balanceAfter(balance.balance()).build());
        }
        return expiredLots.size();
    }
}
