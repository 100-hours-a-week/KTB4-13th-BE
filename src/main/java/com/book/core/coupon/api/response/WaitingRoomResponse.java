package com.book.core.coupon.api.response;

import com.book.core.coupon.application.result.Admission;

public record WaitingRoomResponse(boolean admitted, long position, long estimatedWaitSeconds) {
    public static WaitingRoomResponse from(final Admission admission) {
        return new WaitingRoomResponse(admission.admitted(), admission.position(), admission.estimatedWaitSeconds());
    }
}
