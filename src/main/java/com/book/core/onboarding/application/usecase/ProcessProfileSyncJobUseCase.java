package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort.Job;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import com.book.core.user.application.usecase.CheckWithdrawalConfirmedUseCase;
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
    private final CheckWithdrawalConfirmedUseCase withdrawalConfirmed;
    private final CheckPersonalizationAgreementUseCase agreement;
    private final Clock clock;

    public void execute() {
        jobs.findNext(clock.instant()).ifPresent(this::process);
    }

    private void process(final Job job) {
        try {
            sync(job.userId());
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.AI_SERVICE_INVALID_REQUEST) {
                jobs.fail(job);
                log.atError().addKeyValue("userId", job.userId()).addKeyValue("revision", job.revision())
                    .log("[PROFILE_SYNC] AI가 요청을 거부해 자동 재시도를 멈추고 실패 작업으로 보관합니다. 원인을 고친 뒤 작업을 다시 열어야 합니다.");
                return;
            }
            retry(job, exception);
            return;
        } catch (final DataAccessException | TransactionException exception) {
            retry(job, exception);
            return;
        }
        jobs.complete(job);
    }

    private void sync(final Long userId) {
        if (withdrawalConfirmed.execute(userId)) {
            profiles.executeDelete(userId);
            return;
        }
        if (userRestriction.execute(userId)) {
            return;
        }
        if (!agreement.execute(userId)) {
            profiles.executeDelete(userId);
            return;
        }
        if (onboarding.findActiveByUserId(userId).filter(state -> state.onboardingStatus() == OnboardingStatus.COMPLETED).isPresent()) {
            profiles.executeUpdate(userId);
        }
    }

    /** 401은 회원별 문제가 아닌 서비스 토큰 오류라 작업을 버리지 않고 최대 간격으로만 다시 시도합니다. */
    private void retry(final Job job, final RuntimeException exception) {
        final boolean unauthorized =
            exception instanceof final CoreException coreException && coreException.errorCode() == ErrorCode.AI_SERVICE_UNAUTHORIZED;
        final long delay =
            unauthorized ? MAX_RETRY_SECONDS : Math.min(MAX_RETRY_SECONDS, INITIAL_RETRY_SECONDS << Math.min(job.attempts(), 5));
        jobs.retry(job, clock.instant().plusSeconds(delay));
        (unauthorized ? log.atError() : log.atWarn()).addKeyValue("userId", job.userId()).addKeyValue("attempt", job.attempts() + 1)
            .addKeyValue("errorType", errorType(exception)).log("[PROFILE_SYNC] AI 취향 프로필 동기화를 재예약했습니다.");
    }

    private static String errorType(final RuntimeException exception) {
        if (exception instanceof final CoreException coreException) {
            return coreException.errorCode().name();
        }
        return exception.getClass().getSimpleName();
    }
}
