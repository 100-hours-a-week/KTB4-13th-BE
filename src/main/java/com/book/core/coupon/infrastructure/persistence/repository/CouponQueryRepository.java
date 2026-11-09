package com.book.core.coupon.infrastructure.persistence.repository;

import static com.book.core.coupon.domain.QCoupon.coupon;
import static com.book.core.coupon.domain.QOwnedCoupon.ownedCoupon;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CouponQueryRepository {
    private final JPAQueryFactory queryFactory;

    Optional<CouponIssuanceCounts> findIssuanceCounts(final long couponId) {
        return Optional.ofNullable(queryFactory
            .select(
                Projections.constructor(CouponIssuanceCounts.class, coupon.totalQuantity, coupon.issuedQuantity, ownedCoupon.id.count()))
            .from(coupon).leftJoin(ownedCoupon).on(ownedCoupon.couponId.eq(coupon.id))
            .where(coupon.id.eq(couponId), coupon.deletedAt.isNull()).groupBy(coupon.id, coupon.totalQuantity, coupon.issuedQuantity)
            .fetchOne());
    }
}
