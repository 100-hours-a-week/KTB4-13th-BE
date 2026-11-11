package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.port.WaitingRoomPort;
import com.book.core.coupon.application.result.Admission;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class WaitingRoomUseCaseTest {
    private final WaitingRoomPort room = mock(WaitingRoomPort.class);
    private final WaitingRoomUseCase useCase = new WaitingRoomUseCase(room);
    private final IssueCouponCommand command = new IssueCouponCommand(1L, 123L);

    @Test
    void 일반_쿠폰은_입장권과_요청_한도를_검사하지_않는다() {
        useCase.checkIssuance(command);
        verify(room, never()).retryAfterSeconds(1L);
        assertThatThrownBy(() -> useCase.enter(command)).isInstanceOfSatisfying(CoreException.class,
            e -> assertThat(e.getMessage()).isEqualTo(ErrorCode.COUPON_WAITING_ROOM_NOT_ENTERED.message()));
    }

    @Test
    void 순번을_기준으로_예상_대기_초를_올림한다() {
        when(room.admitPerSecond(123L)).thenReturn(OptionalInt.of(10));
        when(room.enter(123L, 1L)).thenReturn(11L);
        when(room.position(123L, 1L)).thenReturn(11L, 0L, -1L);
        assertThat(useCase.enter(command)).isEqualTo(new Admission(false, 11, 2));
        assertThat(useCase.status(command)).isEqualTo(new Admission(false, 11, 2));
        useCase.checkIssuance(command);
        assertThatThrownBy(() -> useCase.status(command)).isInstanceOf(CoreException.class);
    }

    @Test
    void 요청_한도_초과는_입장권_검사보다_먼저_재요청_시간을_알려준다() {
        when(room.admitPerSecond(123L)).thenReturn(OptionalInt.of(10));
        when(room.retryAfterSeconds(1L)).thenReturn(1L);
        assertThatThrownBy(() -> useCase.checkIssuance(command)).isInstanceOfSatisfying(RetryAfterException.class, e -> {
            assertThat(e.errorCode()).isEqualTo(ErrorCode.COUPON_ISSUE_RATE_LIMITED);
            assertThat(e.retryAfterSeconds()).isOne();
        });
        verify(room, never()).position(123L, 1L);
    }

    @Test
    void 행사_쿠폰은_입장권이_없으면_발급을_거부한다() {
        when(room.admitPerSecond(123L)).thenReturn(OptionalInt.of(10));
        when(room.position(123L, 1L)).thenReturn(1L);
        assertThatThrownBy(() -> useCase.checkIssuance(command)).isInstanceOfSatisfying(CoreException.class,
            e -> assertThat(e.getMessage()).isEqualTo(ErrorCode.COUPON_WAITING_ROOM_PASS_REQUIRED.message()));
    }
}
