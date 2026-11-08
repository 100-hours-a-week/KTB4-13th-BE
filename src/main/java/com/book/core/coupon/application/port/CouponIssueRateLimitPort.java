package com.book.core.coupon.application.port;

public interface CouponIssueRateLimitPort {
    // 0은 허용, 양수는 다음 요청까지 기다려야 하는 초입니다.
    long acquire(Long userId);
}
