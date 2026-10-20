package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.onboarding.application.port.ProfileMemoryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProfileRefreshTargetsUseCase {
    private final ProfileHistoryRepositoryPort historyRepository;
    private final ProfileMemoryRepositoryPort memoryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Long> execute() {
        final ZonedDateTime today = LocalDate.now(clock).atStartOfDay(clock.getZone());
        final Instant from = today.minusDays(1).toInstant();
        final Instant before = today.toInstant();
        return List.copyOf(Stream.of(historyRepository.findChangedUserIds(from, before), memoryRepository.findChangedUserIds(from, before))
            .flatMap(List::stream).collect(TreeSet::new, TreeSet::add, TreeSet::addAll));
    }
}
