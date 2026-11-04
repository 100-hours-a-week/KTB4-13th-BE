package com.book.core.coupon.application.port;

public interface CouponStockPort {
    void initialize(final Long couponId, final int quantity);

    IssueResult tryIssue(final Long couponId);

    enum IssueResult {
        ISSUED,
        SOLD_OUT,
        FAILED
    }
}
