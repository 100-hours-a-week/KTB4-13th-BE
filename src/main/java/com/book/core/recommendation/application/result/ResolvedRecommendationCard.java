package com.book.core.recommendation.application.result;

import java.math.BigDecimal;

// @formatter:off
public record ResolvedRecommendationCard(
        Long bookId,
        Long productId,
        String title,
        String author,
        String coverImageUrl,
        BigDecimal price,
        Integer matchScore,
        String reasonShort,
        String reasonLong) {}
// @formatter:on
