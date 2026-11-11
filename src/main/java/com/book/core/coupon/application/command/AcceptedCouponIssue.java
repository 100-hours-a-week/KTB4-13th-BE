package com.book.core.coupon.application.command;

public record AcceptedCouponIssue(long couponId, long userId, long maxUseCount) {
}
