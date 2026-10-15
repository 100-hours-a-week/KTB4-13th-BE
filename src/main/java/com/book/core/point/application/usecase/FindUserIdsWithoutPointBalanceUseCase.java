package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindUserIdsWithoutPointBalanceUseCase {
    private final PointBalanceRepositoryPort pointBalanceRepository;

    @Transactional(readOnly = true)
    public List<Long> execute(final LocalDateTime registeredBefore, final Long afterUserId, final int limit) {
        return pointBalanceRepository.findActiveUserIdsWithoutPointBalance(registeredBefore, afterUserId, limit);
    }
}
