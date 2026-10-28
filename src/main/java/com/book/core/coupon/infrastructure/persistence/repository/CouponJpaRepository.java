package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByIdInAndDeletedAtIsNull(final Collection<Long> couponIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :couponId AND c.deletedAt IS NULL")
    Optional<Coupon> findByIdAndDeletedAtIsNullForUpdate(final Long couponId);
}
