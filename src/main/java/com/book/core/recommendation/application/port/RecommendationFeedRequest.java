package com.book.core.recommendation.application.port;

import com.book.core.recommendation.application.command.RecommendationFeedSort;
import com.book.core.recommendation.application.command.RecommendationFeedSurface;

// @formatter:off
public record RecommendationFeedRequest(
        Long userId,
        RecommendationFeedSurface surface,
        int size,
        String cursor,
        RecommendationFeedSort sort,
        String category,
        Integer pubYearFrom,
        Integer pubYearTo,
        Integer matchScoreMin) {}
// @formatter:on
