package com.book.core.coupon.application.usecase;

import com.book.common.exception.RetryAfterException;
import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.command.CouponWaitingRoomCommand;
import com.book.core.coupon.application.port.CouponWaitingRoomPort;
import com.book.core.coupon.application.result.CouponWaitingRoomStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CouponWaitingRoomUseCase {
    private final CouponWaitingRoomPort waitingRoom;

    public CouponWaitingRoomStatus enter(final CouponWaitingRoomCommand command) {
        requireEnabled(command.couponId());
        return toStatus(waitingRoom.enter(command.couponId(), command.userId()));
    }

    public CouponWaitingRoomStatus status(final CouponWaitingRoomCommand command) {
        requireEnabled(command.couponId());
        return toStatus(waitingRoom.position(command.couponId(), command.userId()));
    }

    public void requireAdmission(final long couponId, final long userId) {
        if (!waitingRoom.isEnabled(couponId)) {
            return;
        }
        if (!waitingRoom.allowIssueRequest(couponId, userId)) {
            throw new RetryAfterException(ErrorCode.COUPON_REQUEST_RATE_LIMITED, 1);
        }
        if (waitingRoom.position(couponId, userId) != 0) {
            throw new CoreException(ErrorCode.COUPON_WAITING_ROOM_ADMISSION_REQUIRED);
        }
    }

    private void requireEnabled(final long couponId) {
        if (!waitingRoom.isEnabled(couponId)) {
            throw new CoreException(ErrorCode.COUPON_WAITING_ROOM_NOT_ENABLED);
        }
    }

    private CouponWaitingRoomStatus toStatus(final long position) {
        if (position < 0) {
            throw new CoreException(ErrorCode.COUPON_WAITING_ROOM_ADMISSION_REQUIRED);
        }
        return new CouponWaitingRoomStatus(position == 0, position, Math.ceilDiv(position, waitingRoom.admissionsPerSecond()));
    }
}
