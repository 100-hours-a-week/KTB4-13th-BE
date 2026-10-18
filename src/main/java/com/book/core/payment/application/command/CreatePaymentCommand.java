package com.book.core.payment.application.command;

import java.math.BigDecimal;

public record CreatePaymentCommand(Long userId, String orderKey, Long useOwnedCouponId, BigDecimal usePoint) {
}
