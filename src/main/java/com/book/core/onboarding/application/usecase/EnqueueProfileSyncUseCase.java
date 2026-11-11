package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class EnqueueProfileSyncUseCase {
    private final ProfileSyncJobRepositoryPort jobs;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final Long userId) {
        jobs.enqueue(userId, clock.instant());
    }
}
