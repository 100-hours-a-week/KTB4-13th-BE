package com.book.core.recommendation.application.result;

import java.math.BigDecimal;

public record RecommendationFeedItemResult(Long bookId, Long productId, String title, String author, BigDecimal price, String coverUrl,
    boolean inStock, int matchScore) {
}
