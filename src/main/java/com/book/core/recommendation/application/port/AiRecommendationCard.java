package com.book.core.recommendation.application.port;

// @formatter:off
public record AiRecommendationCard(
        Long bookId,
        Integer matchScore,
        String reasonShort,
        String reasonLong) {}
// @formatter:on
