package com.book.core.payment.application.command;

import java.math.BigDecimal;
import java.util.List;

public record CreatePaymentCommand(Long userId, String orderKey, Long useOwnedCouponId, BigDecimal usePoint,
    List<CouponSelectionCommand> couponSelections) {
    public CreatePaymentCommand(final Long userId, final String orderKey, final Long useOwnedCouponId, final BigDecimal usePoint) {
        this(userId, orderKey, useOwnedCouponId, usePoint, List.of());
    }
}
