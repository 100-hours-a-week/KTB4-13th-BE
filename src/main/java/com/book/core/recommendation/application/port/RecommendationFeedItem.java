package com.book.core.recommendation.application.port;

import java.math.BigDecimal;

public record RecommendationFeedItem(Long bookId, String title, String author, BigDecimal price, String coverUrl, boolean inStock,
    int matchScore) {
}
