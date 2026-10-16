package com.book.core.cancel.domain;

import java.math.BigDecimal;

public record CancelCalculated(BigDecimal paidAmount, BigDecimal couponAmount, BigDecimal pointAmount, boolean isRestoreCoupon) {
}
