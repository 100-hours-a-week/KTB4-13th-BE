package com.book.core.recommendation.application.port;

public record AiRecommendationCard(Long bookId, Integer matchScore, String reasonShort, String reasonLong) {
}
