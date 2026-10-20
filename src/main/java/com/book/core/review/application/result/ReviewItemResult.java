package com.book.core.review.application.result;

import com.book.core.review.domain.ReviewTarget;
import java.math.BigDecimal;

public record ReviewItemResult(Long id, ReviewTarget target, BigDecimal rate, String content, boolean isSpoiler, Long userId) {
    public boolean isAuthoredBy(final Long viewerId) {
        return viewerId != null && viewerId.equals(userId);
    }
}
