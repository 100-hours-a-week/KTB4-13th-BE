package com.book.core.coupon.application.port;

import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.domain.CouponIssuePolicy;
import java.util.Optional;
import java.util.function.Supplier;

public interface CouponCacheRepositoryPort {

    Optional<CouponIssueState> getOrLoad(Long couponId, Supplier<Optional<CouponIssuePolicy>> loader);
}
