package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort.Job;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ProcessProfileSyncJobUseCase {
    private static final long INITIAL_RETRY_SECONDS = 30;
    private static final long MAX_RETRY_SECONDS = 900;

    private final ProfileSyncJobRepositoryPort jobs;
    private final UserOnboardingRepositoryPort onboarding;
    private final PersonalizationProfileUseCase profiles;
    private final CheckUserRestrictionUseCase userRestriction;
    private final Clock clock;

    public void execute() {
        jobs.findNext(clock.instant()).ifPresent(this::process);
    }

    private void process(final Job job) {
        try {
            if (!userRestriction.execute(job.userId()) && onboarding.findActiveByUserId(job.userId())
                .filter(state -> state.onboardingStatus() == OnboardingStatus.COMPLETED).isPresent()) {
                profiles.executeUpdate(job.userId(), job.revision());
            }
        } catch (final CoreException | DataAccessException | TransactionException exception) {
            final long delay = Math.min(MAX_RETRY_SECONDS, INITIAL_RETRY_SECONDS << Math.min(job.attempts(), 5));
            // shortcut: 현재 AI 클라이언트는 실패 원인을 묶어 전달하므로 상한 간격으로 재시도하고, 오류 분류 계약이 생기면 영구 실패를 분리합니다.
            jobs.retry(job, clock.instant().plusSeconds(delay));
            log.atWarn().addKeyValue("userId", job.userId()).addKeyValue("attempt", job.attempts() + 1)
                .addKeyValue("errorType", exception.getClass().getSimpleName()).log("[PROFILE_SYNC] AI 취향 프로필 갱신을 재예약했습니다.");
            return;
        }
        jobs.complete(job);
    }
}
