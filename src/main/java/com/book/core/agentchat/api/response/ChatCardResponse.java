package com.book.core.agentchat.api.response;

import java.math.BigDecimal;

public record ChatCardResponse(Long recommendationCardId, Long bookId, Long productId, String title, String author, String coverImageUrl,
    BigDecimal price, Integer matchScore, String reasonShort, String reasonLong) {
}
