package com.book.core.coupon.application.command;

public record IssuanceRequested(long couponId, long userId, long maxUseCount) {
}
