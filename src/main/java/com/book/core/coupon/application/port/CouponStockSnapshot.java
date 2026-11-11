package com.book.core.coupon.application.port;

import java.util.Set;

public record CouponStockSnapshot(Integer quantity, boolean soldOutSignalPresent, Set<Long> reservedUserIds) {
}
