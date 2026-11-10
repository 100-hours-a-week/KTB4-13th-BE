package com.book.core.onboarding.application.usecase;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort.Job;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessProfileSyncJobUseCaseTest {
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-11-09T01:00:00Z");

    private final ProfileSyncJobRepositoryPort jobs = mock(ProfileSyncJobRepositoryPort.class);
    private final UserOnboardingRepositoryPort onboarding = mock(UserOnboardingRepositoryPort.class);
    private final PersonalizationProfileUseCase profiles = mock(PersonalizationProfileUseCase.class);
    private final CheckUserRestrictionUseCase userRestriction = mock(CheckUserRestrictionUseCase.class);
    private final ProcessProfileSyncJobUseCase process =
        new ProcessProfileSyncJobUseCase(jobs, onboarding, profiles, userRestriction, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 장기_장애에도_작업을_유지하고_재시도_간격을_15분으로_제한한다() {
        final Job job = new Job(USER_ID, UUID.randomUUID(), 12);
        when(jobs.findNext(NOW)).thenReturn(Optional.of(job));
        when(onboarding.findActiveByUserId(USER_ID)).thenReturn(
            Optional.of(new UserOnboarding(1L, USER_ID, OnboardingStatus.COMPLETED, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))));
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(profiles).executeUpdate(USER_ID, job.revision());

        process.execute();

        verify(jobs).retry(job, NOW.plusSeconds(900));
    }

    @Test
    void 탈퇴를_요청한_사용자의_작업은_AI에_전달하지_않고_정리한다() {
        final Job job = new Job(USER_ID, UUID.randomUUID(), 0);
        when(jobs.findNext(NOW)).thenReturn(Optional.of(job));
        when(userRestriction.execute(USER_ID)).thenReturn(true);

        process.execute();

        verifyNoInteractions(profiles);
        verify(jobs).complete(job);
    }
}
