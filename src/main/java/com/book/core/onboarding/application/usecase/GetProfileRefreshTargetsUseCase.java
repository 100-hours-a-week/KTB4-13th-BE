package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProfileRefreshTargetsUseCase {
    private final ProfileHistoryRepositoryPort historyRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Long> execute() {
        final ZonedDateTime today = LocalDate.now(clock).atStartOfDay(clock.getZone());
        return historyRepository.findChangedUserIds(today.minusDays(1).toInstant(), today.toInstant());
    }
}
