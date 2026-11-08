package com.book.core.coupon.application.result;

import com.book.core.coupon.domain.CouponIssuePolicy;

public record CouponIssueState(CouponIssuePolicy policy, Stock stock) {
    public enum Stock {
        UNKNOWN,
        AVAILABLE,
        EXHAUSTED
    }
}
