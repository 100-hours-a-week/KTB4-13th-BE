package com.book.core.coupon.application.port;

public record CouponStockSnapshot(Integer quantity, boolean soldOutSignalPresent) {
}
