package com.book.core.coupon.application.port;

/** A null quantity means the Redis stock key is missing. */
public record CouponStockSnapshot(Integer quantity, boolean soldOutSignalPresent) {
}
