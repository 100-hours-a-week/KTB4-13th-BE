package com.book.core.onboarding.application.port;

import java.util.List;

// @formatter:off
public record AiPersonalizationProfileRequest(
        Long userId,
        List<String> readingTimes,
        List<String> criteria,
        List<String> categories,
        List<String> tags,
        List<Long> likedBookIds,
        List<AiPersonalizationProfileMemory> memories,
        AiPersonalizationProfileHistory history) {
    public AiPersonalizationProfileRequest(final Long userId, final List<String> readingTimes, final List<String> criteria,
            final List<String> categories, final List<String> tags, final List<Long> likedBookIds) {
        this(userId, readingTimes, criteria, categories, tags, likedBookIds, List.of(), AiPersonalizationProfileHistory.EMPTY);
    }
}
// @formatter:on
