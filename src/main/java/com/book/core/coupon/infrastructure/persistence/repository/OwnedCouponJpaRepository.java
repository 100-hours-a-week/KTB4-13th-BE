package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.OwnedCoupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedCouponJpaRepository extends JpaRepository<OwnedCoupon, Long> {
    Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId);

    List<OwnedCoupon> findByUserIdAndDeletedAtIsNull(final Long userId);

}
