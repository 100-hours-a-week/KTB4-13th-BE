package com.book.core.coupon.infrastructure.persistence.repository;

import static com.book.core.coupon.domain.QOwnedCoupon.ownedCoupon;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class OwnedCouponQueryRepository {
    private final JPAQueryFactory queryFactory;

    boolean existsByUserIdAndCouponId(final Long userId, final Long couponId) {
        return queryFactory.selectOne().from(ownedCoupon).where(ownedCoupon.userId.eq(userId), ownedCoupon.couponId.eq(couponId))
            .fetchFirst() != null;
    }
}
