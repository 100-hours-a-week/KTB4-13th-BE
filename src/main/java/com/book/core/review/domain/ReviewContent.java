package com.book.core.review.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReviewContent(BigDecimal rate, String content, Boolean isSpoiler) {
    private static final BigDecimal RATE_UNIT = new BigDecimal("0.5");

    public ReviewContent {
        if (rate == null || rate.compareTo(RATE_UNIT) < 0 || rate.compareTo(BigDecimal.valueOf(5)) > 0
            || rate.remainder(RATE_UNIT).compareTo(BigDecimal.ZERO) != 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (content == null || content.isBlank() || content.length() > 255) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        rate = rate.setScale(1, RoundingMode.UNNECESSARY);
    }
}
