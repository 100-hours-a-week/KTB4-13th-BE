package com.book.core.recommendation.application.port;

import java.math.BigDecimal;

// @formatter:off
public record AiRecommendationCard(
        Long bookId,
        Integer matchScore,
        BigDecimal price,
        String reasonShort,
        String reasonLong) {}
// @formatter:on
