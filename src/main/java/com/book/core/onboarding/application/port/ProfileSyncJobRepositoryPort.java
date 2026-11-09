package com.book.core.onboarding.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProfileSyncJobRepositoryPort {
    void enqueue(Long userId, Instant availableAt);

    Optional<Job> findNext(Instant now);

    void complete(Job job);

    void retry(Job job, Instant availableAt);

    record Job(Long userId, UUID revision, int attempts) {
    }
}
