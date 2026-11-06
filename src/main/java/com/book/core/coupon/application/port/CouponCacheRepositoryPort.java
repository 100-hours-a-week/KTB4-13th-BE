package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.CouponIssuePolicy;
import java.util.function.Supplier;

public interface CouponCacheRepositoryPort {

    CouponIssuePolicy getOrLoad(final Long couponId, final Supplier<CouponIssuePolicy> loader);
}
