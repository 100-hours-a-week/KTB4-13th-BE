package com.book.core.onboarding.application.port;

import java.util.List;

// @formatter:off
public record AiPersonalizationProfileRequest(
        Long userId,
        List<String> readingTimes,
        List<String> criteria,
        List<String> categories,
        List<String> tags,
        List<Long> likedBookIds) {}
// @formatter:on
