package com.book.core.point.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;

public final class PointAmount {
    public static final BigDecimal TEXT_REVIEW = BigDecimal.valueOf(1000);
    public static final BigDecimal IMAGE_REVIEW = BigDecimal.valueOf(1500);
    public static final BigDecimal PAYMENT = BigDecimal.valueOf(2000);

    private PointAmount() {}

    public static void requireWhole(final BigDecimal amount) {
        if (amount == null || amount.stripTrailingZeros().scale() > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

}
