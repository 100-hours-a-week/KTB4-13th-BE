package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointLotRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GetPointBalanceUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;
    private final PointLotRepositoryPort pointLotRepository;
    private final Clock clock;

    public GetPointBalanceUseCase(final PointBalanceRepositoryPort pointBalanceRepository) {
        this(pointBalanceRepository, null, Clock.systemDefaultZone());
    }

    @Transactional(readOnly = true)
    public BigDecimal execute(final Long userId) {
        final var balance = pointBalanceRepository.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (balance.balance().signum() < 0) {
            return balance.balance();
        }
        return pointLotRepository == null ? balance.balance() : pointLotRepository.sumAvailableByUserId(userId, LocalDateTime.now(clock));
    }
}
