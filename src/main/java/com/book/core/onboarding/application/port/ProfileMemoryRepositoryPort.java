package com.book.core.onboarding.application.port;

import java.util.List;

public interface ProfileMemoryRepositoryPort {
    List<AiPersonalizationProfileMemory> findLatestActive(final Long userId, final int limit);
}
