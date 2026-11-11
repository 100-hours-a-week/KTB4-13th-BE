package com.book.core.coupon.application.result;

public record Admission(boolean admitted, long position, long estimatedWaitSeconds) {
    public static Admission from(final long position, final int admitPerSecond) {
        return new Admission(position == 0, position, Math.ceilDiv(position, admitPerSecond));
    }
}
