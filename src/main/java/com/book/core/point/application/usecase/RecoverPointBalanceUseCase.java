package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RecoverPointBalanceUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;

    @Transactional
    public boolean execute(final Long userId) {
        if (pointBalanceRepository.existsByUserId(userId)) {
            return false;
        }
        pointBalanceRepository.save(new PointBalance(userId));
        return true;
    }
}
