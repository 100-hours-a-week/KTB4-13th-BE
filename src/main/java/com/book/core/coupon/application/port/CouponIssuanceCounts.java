package com.book.core.coupon.application.port;

import java.util.Set;

public record CouponIssuanceCounts(int totalQuantity, int issuedQuantity, Set<Long> issuedUserIds) {
    public long ownedCouponCount() {
        return issuedUserIds.size();
    }
}
