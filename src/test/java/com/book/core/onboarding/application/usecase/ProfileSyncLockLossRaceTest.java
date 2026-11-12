package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ProfileSyncLockLossRaceTest {
    @Test
    void 락_연결_상실을_모의하면_이전_작업자가_DELETE_뒤에_UPSERT할_수_있다() throws Exception {
        final var jobs = mock(ProfileSyncJobRepositoryPort.class);
        final var onboarding = mock(UserOnboardingRepositoryPort.class);
        final var profiles = mock(PersonalizationProfileUseCase.class);
        final var restricted = mock(CheckUserRestrictionUseCase.class);
        final var withdrawn = mock(CheckWithdrawalConfirmedUseCase.class);
        final var agreement = mock(CheckPersonalizationAgreementUseCase.class);
        final var now = Instant.parse("2026-11-09T01:00:00Z");
        final var first = new ProfileSyncJobRepositoryPort.Job(42L, UUID.randomUUID(), 0);
        final var second = new ProfileSyncJobRepositoryPort.Job(42L, UUID.randomUUID(), 0);
        final var confirmed = new AtomicBoolean(false);
        final var entered = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var events = new CopyOnWriteArrayList<String>();
        when(jobs.findNext(now)).thenReturn(Optional.of(first), Optional.of(second));
        when(withdrawn.execute(42L)).thenAnswer(invocation -> confirmed.get());
        when(agreement.execute(42L)).thenReturn(true);
        when(onboarding.findActiveByUserId(42L))
            .thenReturn(Optional.of(new UserOnboarding(1L, 42L, OnboardingStatus.COMPLETED, LocalDateTime.ofInstant(now, ZoneOffset.UTC))));
        doAnswer(invocation -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("worker A did not resume");
            }
            events.add("UPSERT");
            return null;
        }).when(profiles).executeUpdate(eq(42L), any());
        doAnswer(invocation -> {
            events.add("DELETE");
            return null;
        }).when(profiles).executeDelete(eq(42L), any());
        final var process = new ProcessProfileSyncJobUseCase(jobs, onboarding, profiles, restricted, withdrawn, agreement,
            Clock.fixed(now, ZoneOffset.UTC));

        try (var executor = Executors.newSingleThreadExecutor()) {
            final var workerA = executor.submit(process::execute);
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            confirmed.set(true);
            // A의 MySQL 락 연결이 끊겨 B가 진입한 상황을 모의합니다. 실제 DB 장애 주입은 아닙니다.
            process.execute();
            release.countDown();
            workerA.get(5, TimeUnit.SECONDS);
        }
        assertThat(events).containsExactly("DELETE", "UPSERT");
    }
}
