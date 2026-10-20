package com.book.core.onboarding.application.port;

import java.time.Instant;
import java.util.List;

public interface ProfileMemoryRepositoryPort {
    List<AiPersonalizationProfileMemory> findLatestActive(final Long userId, final int limit);

    List<Long> findChangedUserIds(final Instant from, final Instant before);
}
