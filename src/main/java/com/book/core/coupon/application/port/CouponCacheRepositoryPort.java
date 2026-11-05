package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.CouponIssuePolicy;
import java.util.Optional;

public interface CouponCacheRepositoryPort {
    Optional<CouponIssuePolicy> find(final Long couponId);

    void save(final Long couponId, final CouponIssuePolicy policy);
}
