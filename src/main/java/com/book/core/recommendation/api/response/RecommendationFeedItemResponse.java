package com.book.core.recommendation.api.response;

import com.book.core.recommendation.application.result.RecommendationFeedItemResult;
import java.math.BigDecimal;

// @formatter:off
public record RecommendationFeedItemResponse(
        Long bookId,
        Long productId,
        String title,
        String author,
        BigDecimal price,
        String coverUrl,
        boolean inStock,
        int matchScore) {
    public static RecommendationFeedItemResponse from(final RecommendationFeedItemResult item) {
        return new RecommendationFeedItemResponse(item.bookId(), item.productId(), item.title(), item.author(), item.price(),
                item.coverUrl(), item.inStock(), item.matchScore());
    }
}
// @formatter:on
