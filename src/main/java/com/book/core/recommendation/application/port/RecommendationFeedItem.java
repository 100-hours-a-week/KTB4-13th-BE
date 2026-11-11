package com.book.core.recommendation.application.port;

import java.math.BigDecimal;

// @formatter:off
public record RecommendationFeedItem(
        Long bookId,
        String title,
        String author,
        BigDecimal price,
        String coverUrl,
        int matchScore) {}
// @formatter:on
