package com.book.core.recommendation.application.port;

public record RecommendationFeedRefreshResult(int refreshedCount, int remainingCount) {
}
