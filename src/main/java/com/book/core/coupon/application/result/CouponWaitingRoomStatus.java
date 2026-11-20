package com.book.core.coupon.application.result;

public record CouponWaitingRoomStatus(boolean admitted, long position, long estimatedWaitSeconds) {
}
