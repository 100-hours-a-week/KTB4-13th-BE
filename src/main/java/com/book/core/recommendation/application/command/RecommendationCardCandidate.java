package com.book.core.recommendation.application.command;



public record RecommendationCardCandidate(Long bookId, Integer matchScore, String reasonShort, String reasonLong) {
}
