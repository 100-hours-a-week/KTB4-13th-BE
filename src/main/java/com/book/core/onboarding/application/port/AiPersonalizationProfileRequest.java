package com.book.core.onboarding.application.port;

import java.util.List;

public record AiPersonalizationProfileRequest(Long userId, List<String> readingTimes, List<String> criteria, List<String> categories,
    List<String> tags, List<Long> likedBookIds) {
}
