package com.book.core.coupon.application.usecase;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.port.WaitingRoomPort;
import com.book.core.coupon.application.result.Admission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaitingRoomUseCase {
    private final WaitingRoomPort waitingRoom;

    public Admission enter(final IssueCouponCommand command) {
        final int rate = requireRoom(command.couponId());
        checkRateLimit(command.userId());
        return Admission.from(waitingRoom.enter(command.couponId(), command.userId()), rate);
    }

    public Admission status(final IssueCouponCommand command) {
        final int rate = requireRoom(command.couponId());
        checkRateLimit(command.userId());
        final long position = waitingRoom.position(command.couponId(), command.userId());
        if (position < 0) {
            throw new CoreException(ErrorCode.COUPON_WAITING_ROOM_NOT_ENTERED);
        }
        return Admission.from(position, rate);
    }

    public void checkIssuance(final IssueCouponCommand command) {
        if (waitingRoom.admitPerSecond(command.couponId()).isEmpty()) {
            return;
        }
        checkRateLimit(command.userId());
        if (waitingRoom.position(command.couponId(), command.userId()) != 0) {
            throw new CoreException(ErrorCode.COUPON_WAITING_ROOM_PASS_REQUIRED);
        }
    }

    private int requireRoom(final long couponId) {
        return waitingRoom.admitPerSecond(couponId).orElseThrow(() -> new CoreException(ErrorCode.COUPON_WAITING_ROOM_NOT_ENTERED));
    }

    private void checkRateLimit(final long userId) {
        final long retryAfter = waitingRoom.retryAfterSeconds(userId);
        if (retryAfter > 0) {
            throw new RetryAfterException(ErrorCode.COUPON_ISSUE_RATE_LIMITED, retryAfter);
        }
    }
}
