package com.book.core.coupon.infrastructure.persistence.repository;

import com.book.core.coupon.domain.Coupon;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByIdInAndDeletedAtIsNull(final Collection<Long> couponIds);

    Optional<Coupon> findByIdAndDeletedAtIsNull(final Long couponId);
}
