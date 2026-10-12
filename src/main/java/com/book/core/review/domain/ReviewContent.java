package com.book.core.review.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReviewContent(BigDecimal rate, String content) {
    public ReviewContent {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.TEN) > 0 || content == null || content.length() > 255) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        try {
            rate = rate.setScale(1, RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
