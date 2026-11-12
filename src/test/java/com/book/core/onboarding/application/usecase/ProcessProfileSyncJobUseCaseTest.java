package com.book.core.onboarding.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.book.core.user.application.usecase.CheckWithdrawalConfirmedUseCase;
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
    private final CheckWithdrawalConfirmedUseCase withdrawalConfirmed = mock(CheckWithdrawalConfirmedUseCase.class);
    private final CheckPersonalizationAgreementUseCase agreement = mock(CheckPersonalizationAgreementUseCase.class);
    private final ProcessProfileSyncJobUseCase process = new ProcessProfileSyncJobUseCase(jobs, onboarding, profiles, userRestriction,
        withdrawalConfirmed, agreement, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 장기_장애에도_작업을_유지하고_재시도_간격을_15분으로_제한한다() {
        final Job job = givenJob(12);
        givenAgreedAndCompleted();
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(profiles).executeUpdate(USER_ID, job.revision());

        process.execute();

        verify(jobs).retry(job, NOW.plusSeconds(900));
    }

    @Test
    void 복구_기간_중인_탈퇴_신청_회원은_프로필을_갱신하지도_삭제하지도_않고_작업을_정리한다() {
        final Job job = givenJob(0);
        when(userRestriction.execute(USER_ID)).thenReturn(true);

        process.execute();

        verifyNoInteractions(profiles);
        verify(jobs).complete(job);
    }

    @Test
    void 최종_탈퇴가_확정된_회원은_동의_여부와_관계없이_프로필을_삭제한다() {
        final Job job = givenJob(0);
        when(withdrawalConfirmed.execute(USER_ID)).thenReturn(true);
        when(agreement.execute(USER_ID)).thenReturn(true);

        process.execute();

        verify(profiles).executeDelete(USER_ID);
        verify(profiles, never()).executeUpdate(USER_ID, job.revision());
        verify(jobs).complete(job);
    }

    @Test
    void 처리_시점에_동의가_없으면_프로필_계산_대신_삭제한다() {
        final Job job = givenJob(0);
        when(onboarding.findActiveByUserId(USER_ID)).thenReturn(Optional.of(completedOnboarding()));

        process.execute();

        verify(profiles).executeDelete(USER_ID);
        verify(profiles, never()).executeUpdate(USER_ID, job.revision());
        verify(jobs).complete(job);
    }

    @Test
    void AI가_400으로_거부한_요청은_자동_재시도하지_않되_완료로_지우지_않고_실패_작업으로_보관한다() {
        final Job job = givenJob(0);
        when(withdrawalConfirmed.execute(USER_ID)).thenReturn(true);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_INVALID_REQUEST)).when(profiles).executeDelete(USER_ID);

        process.execute();

        verify(jobs).fail(job);
        verify(jobs, never()).complete(any());
        verify(jobs, never()).retry(any(), any());
    }

    @Test
    void 메시지_크기_초과는_다시_보내도_같으므로_갱신_작업을_재시도하지_않고_실패_작업으로_보관한다() {
        final Job job = givenJob(0);
        givenAgreedAndCompleted();
        doThrow(new CoreException(ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE)).when(profiles).executeUpdate(USER_ID, job.revision());

        process.execute();

        verify(jobs).fail(job);
        verify(jobs, never()).retry(any(), any());
        verify(jobs, never()).complete(any());
    }

    @Test
    void 갱신_요청의_400과_401은_기존처럼_첫_재시도를_30초_뒤로_예약한다() {
        final Job job = givenJob(0);
        givenAgreedAndCompleted();
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).doThrow(new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED))
            .when(profiles).executeUpdate(USER_ID, job.revision());

        process.execute();
        process.execute();

        verify(jobs, times(2)).retry(job, NOW.plusSeconds(30));
        verify(jobs, never()).fail(any());
    }

    @Test
    void 서비스_토큰_인증_실패는_삭제_작업을_버리지_않고_최대_간격으로만_다시_시도한다() {
        final Job job = givenJob(0);
        when(withdrawalConfirmed.execute(USER_ID)).thenReturn(true);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED)).when(profiles).executeDelete(USER_ID);

        process.execute();

        verify(jobs).retry(job, NOW.plusSeconds(900));
        verify(jobs, never()).complete(job);
    }

    private Job givenJob(final int attempts) {
        final Job job = new Job(USER_ID, UUID.randomUUID(), attempts);
        when(jobs.findNext(NOW)).thenReturn(Optional.of(job));
        return job;
    }

    private void givenAgreedAndCompleted() {
        when(agreement.execute(USER_ID)).thenReturn(true);
        when(onboarding.findActiveByUserId(USER_ID)).thenReturn(Optional.of(completedOnboarding()));
    }

    private static UserOnboarding completedOnboarding() {
        return new UserOnboarding(1L, USER_ID, OnboardingStatus.COMPLETED, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }
}
