package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.coupon.application.port.CouponIssueRateLimitPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CheckCouponIssueRateLimitUseCase {
    private final CouponIssueRateLimitPort rateLimit;

    public void execute(final Long userId) {
        final long retryAfterSeconds = rateLimit.acquire(userId);
        if (retryAfterSeconds > 0) {
            throw new RetryAfterException(ErrorCode.COUPON_ISSUE_RATE_LIMITED, retryAfterSeconds);
        }
    }
}
