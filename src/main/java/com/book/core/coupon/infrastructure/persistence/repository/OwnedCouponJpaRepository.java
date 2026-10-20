package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OwnedCouponJpaRepository extends JpaRepository<OwnedCoupon, Long> {
    Optional<OwnedCoupon> findByUserIdAndCouponId(final Long userId, final Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OwnedCoupon> findByIdAndDeletedAtIsNull(final Long ownedCouponId);

    List<OwnedCoupon> findByUserIdAndDeletedAtIsNull(final Long userId);

    @Query("""
        SELECT DISTINCT ownedCoupon FROM OwnedCoupon ownedCoupon
        JOIN Coupon coupon ON ownedCoupon.couponId = coupon.id
            AND ownedCoupon.userId = :userId
            AND ownedCoupon.status = :status
            AND ownedCoupon.deletedAt IS NULL
        WHERE coupon.id IN :couponIds
            AND coupon.deletedAt IS NULL
            AND coupon.expiredAt > :expiredAtAfter
        """)
    List<OwnedCoupon> findOwnedCouponIds(final Long userId, final Collection<Long> couponIds, final LocalDateTime expiredAtAfter,
        final OwnedCouponStatus status);
}
