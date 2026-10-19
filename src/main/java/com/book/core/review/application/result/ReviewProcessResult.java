package com.book.core.review.application.result;

import com.book.core.point.domain.PointAmount;
import com.book.core.review.domain.ReviewFormat;
import java.math.BigDecimal;

public record ReviewProcessResult(Long id, ReviewFormat format) {
    public BigDecimal pointAmount() {
        return switch (format) {
            case TEXT -> PointAmount.TEXT_REVIEW;
            case IMAGE -> PointAmount.IMAGE_REVIEW;
        };
    }
}
