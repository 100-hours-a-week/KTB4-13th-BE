package com.book.core.coupon.application.port;

import java.util.OptionalInt;

public interface WaitingRoomPort {
    OptionalInt admitPerSecond(long couponId);

    long enter(long couponId, long userId);

    long position(long couponId, long userId);

    long retryAfterSeconds(long userId);
}
