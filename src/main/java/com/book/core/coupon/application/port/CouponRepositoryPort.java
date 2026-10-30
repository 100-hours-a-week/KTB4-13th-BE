package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.Coupon;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponRepositoryPort {
    List<Coupon> findActiveByIds(final Collection<Long> couponIds);

    Optional<Coupon> findByIdWithLock(final Long couponId);

    Optional<Coupon> findById(final Long couponId);

    List<Coupon> findAllById(final Collection<Long> couponIds);
}
