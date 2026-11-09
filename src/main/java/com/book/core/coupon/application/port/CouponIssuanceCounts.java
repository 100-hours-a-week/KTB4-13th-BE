package com.book.core.coupon.application.port;

public record CouponIssuanceCounts(int totalQuantity, int issuedQuantity, long ownedCouponCount) {
}
