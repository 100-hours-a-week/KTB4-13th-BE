package com.book.core.coupon.infrastructure.persistence.repository;

import static com.book.core.coupon.domain.QCoupon.coupon;
import static com.book.core.coupon.domain.QOwnedCoupon.ownedCoupon;

import com.book.core.coupon.application.port.CouponIssuanceCounts;
import java.util.Objects;
import java.util.stream.Collectors;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CouponQueryRepository {
    private final JPAQueryFactory queryFactory;

    Optional<CouponIssuanceCounts> findIssuanceCounts(final long couponId) {
        final var rows = queryFactory.select(coupon.totalQuantity, coupon.issuedQuantity, ownedCoupon.userId).from(coupon)
            .leftJoin(ownedCoupon).on(ownedCoupon.couponId.eq(coupon.id)).where(coupon.id.eq(couponId), coupon.deletedAt.isNull()).fetch();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        final var first = rows.getFirst();
        return Optional.of(new CouponIssuanceCounts(first.get(coupon.totalQuantity), first.get(coupon.issuedQuantity),
            rows.stream().map(row -> row.get(ownedCoupon.userId)).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet())));
    }
}
