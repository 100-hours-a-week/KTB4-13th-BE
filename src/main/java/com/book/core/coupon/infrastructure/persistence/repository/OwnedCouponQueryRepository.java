package com.book.core.coupon.infrastructure.persistence.repository;

import static com.book.core.coupon.domain.QCoupon.coupon;
import static com.book.core.coupon.domain.QOwnedCoupon.ownedCoupon;

import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    List<OwnedCoupon> findUsable(final Long userId, final LocalDateTime now) {
        return usableCoupons(userId, now).fetch();
    }

    List<OwnedCoupon> findUsable(final Long userId, final Collection<Long> couponIds, final LocalDateTime now) {
        return usableCoupons(userId, now).where(coupon.id.in(couponIds)).fetch();
    }

    private JPAQuery<OwnedCoupon> usableCoupons(final Long userId, final LocalDateTime now) {
        return queryFactory.selectDistinct(ownedCoupon).from(ownedCoupon).join(coupon).on(ownedCoupon.couponId.eq(coupon.id)).where(
            ownedCoupon.userId.eq(userId), ownedCoupon.status.eq(OwnedCouponStatus.ISSUED), ownedCoupon.deletedAt.isNull(),
            coupon.deletedAt.isNull(), coupon.expiredAt.gt(now));
    }
}
