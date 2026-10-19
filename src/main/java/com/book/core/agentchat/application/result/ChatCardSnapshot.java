package com.book.core.agentchat.application.result;

import com.book.core.recommendation.application.result.RecommendationCardResult;
import java.math.BigDecimal;

public record ChatCardSnapshot(Long recommendationCardId, Long bookId, Long productId, String title, String author, String coverImageUrl,
    BigDecimal price, Integer matchScore, String reasonShort, String reasonLong) {
    public static ChatCardSnapshot from(final RecommendationCardResult card) {
        return new ChatCardSnapshot(card.recommendationCardId(), card.bookId(), card.productId(), card.title(), card.author(),
            card.coverImageUrl(), card.price(), card.matchScore(), card.reasonShort(), card.reasonLong());
    }
}
