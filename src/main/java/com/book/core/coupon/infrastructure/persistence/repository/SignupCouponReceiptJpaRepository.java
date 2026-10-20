package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.SignupCouponReceipt;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

interface SignupCouponReceiptJpaRepository extends JpaRepository<SignupCouponReceipt, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignupCouponReceipt> findByCouponIdAndProviderEmail(final Long couponId, final String providerEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignupCouponReceipt> findByCouponIdAndUserId(final Long couponId, final Long userId);

    @Modifying
    @Query("DELETE FROM SignupCouponReceipt receipt WHERE receipt.expiresAt IS NOT NULL AND receipt.expiresAt <= :now")
    int deleteExpiredBefore(@Param("now") final LocalDateTime now);
}
