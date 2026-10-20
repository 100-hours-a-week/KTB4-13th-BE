package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.onboarding.application.port.ProfileMemoryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetProfileRefreshTargetsUseCaseTest {
    private final ProfileHistoryRepositoryPort historyRepository = mock(ProfileHistoryRepositoryPort.class);
    private final ProfileMemoryRepositoryPort memoryRepository = mock(ProfileMemoryRepositoryPort.class);

    @Test
    void 서비스_시간대의_전날_0시부터_오늘_0시_전까지_이력이나_취향_기억이_바뀐_회원을_중복_없이_조회한다() {
        final Clock clock = Clock.fixed(Instant.parse("2026-10-20T17:00:00Z"), ZoneId.of("Asia/Seoul"));
        final Instant from = Instant.parse("2026-10-19T15:00:00Z");
        final Instant before = Instant.parse("2026-10-20T15:00:00Z");
        when(historyRepository.findChangedUserIds(from, before)).thenReturn(List.of(7L, 3L));
        when(memoryRepository.findChangedUserIds(from, before)).thenReturn(List.of(7L, 5L));

        assertThat(new GetProfileRefreshTargetsUseCase(historyRepository, memoryRepository, clock).execute()).containsExactly(3L, 5L, 7L);
    }
}
