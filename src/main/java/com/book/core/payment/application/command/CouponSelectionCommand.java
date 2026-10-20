package com.book.core.payment.application.command;

public record CouponSelectionCommand(Long orderItemId, Long ownedCouponId) {
}
