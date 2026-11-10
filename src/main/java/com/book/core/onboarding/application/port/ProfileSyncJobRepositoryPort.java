package com.book.core.onboarding.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProfileSyncJobRepositoryPort {
    void enqueue(Long userId, Instant availableAt);

    Optional<Job> findNext(Instant now);

    void complete(Job job);

    void retry(Job job, Instant availableAt);

    /** 자동 재시도로 해결되지 않는 작업을 지우지 않고 보관합니다. 같은 회원의 새 예약이나 운영자의 재개로 다시 처리됩니다. */
    void fail(Job job);

    record Job(Long userId, UUID revision, int attempts) {
    }
}
