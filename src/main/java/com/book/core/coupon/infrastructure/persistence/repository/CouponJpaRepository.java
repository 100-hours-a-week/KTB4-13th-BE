package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import com.book.core.coupon.domain.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByIdInAndDeletedAtIsNull(final Collection<Long> couponIds);

    Optional<Coupon> findByIdAndDeletedAtIsNull(final Long couponId);

    @Transactional(readOnly = true)
    @Query("""
        SELECT new com.book.core.coupon.application.port.CouponIssuanceCounts(
            c.totalQuantity, c.issuedQuantity, COUNT(o.id))
        FROM Coupon c LEFT JOIN OwnedCoupon o ON o.couponId = c.id
        WHERE c.id = :couponId AND c.deletedAt IS NULL
        GROUP BY c.id, c.totalQuantity, c.issuedQuantity
        """)
    Optional<CouponIssuanceCounts> findIssuanceCounts(@Param("couponId") final long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :couponId and c.deletedAt is null")
    Optional<Coupon> findByIdForUpdate(@Param("couponId") final Long couponId);

    @Modifying
    @Query("""
        UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + 1
        WHERE c.id = :couponId AND c.deletedAt IS NULL AND c.issuedQuantity < c.totalQuantity
        """)
    int incrementIssuedQuantity(@Param("couponId") Long couponId);
}
