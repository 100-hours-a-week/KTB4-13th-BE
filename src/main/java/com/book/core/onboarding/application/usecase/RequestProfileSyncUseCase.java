package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 작업자가 처리 시점 상태로 갱신·삭제를 정하므로, 프로필을 지울 상태는 예약이 처리되기 전에 확정돼 있어야 합니다. */
@UseCase
@RequiredArgsConstructor
public class RequestProfileSyncUseCase {
    private final ProfileSyncJobRepositoryPort profileJobs;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId) {
        profileJobs.enqueue(userId, clock.instant());
    }
}
