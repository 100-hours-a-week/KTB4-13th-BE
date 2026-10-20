package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.SignupCouponQuota;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface SignupCouponQuotaJpaRepository extends JpaRepository<SignupCouponQuota, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignupCouponQuota> findByCouponId(final Long couponId);
}
