package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.OwnedCoupon;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OwnedCouponRepositoryPort {
    Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId);

    List<OwnedCoupon> findActiveByUserId(final Long userId);

    Optional<OwnedCoupon> findById(final Long ownedCouponId);

    default Optional<OwnedCoupon> findByIdForUpdate(final Long ownedCouponId) {
        return findById(ownedCouponId);
    }

    List<OwnedCoupon> findOwnedCouponIds(final Long userId, final Collection<Long> couponIds, final LocalDateTime expiredAtAfter);

    OwnedCoupon save(final OwnedCoupon ownedCoupon);
}
