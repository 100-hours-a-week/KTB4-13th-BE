package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPointBalanceUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;

    @Transactional(readOnly = true)
    public BigDecimal execute(final Long userId) {
        return pointBalanceRepository.findByUserId(userId).map(PointBalance::balance).orElse(BigDecimal.ZERO);
    }
}
