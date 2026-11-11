package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.RetryAfterException;
import com.book.core.coupon.application.command.CouponWaitingRoomCommand;
import com.book.core.coupon.application.port.CouponWaitingRoomPort;
import org.junit.jupiter.api.Test;

class CouponWaitingRoomUseCaseTest {
    private final CouponWaitingRoomPort room = mock(CouponWaitingRoomPort.class);
    private final CouponWaitingRoomUseCase useCase = new CouponWaitingRoomUseCase(room);
    private final CouponWaitingRoomCommand command = new CouponWaitingRoomCommand(42L, 10L);

    @Test
    void 일반_쿠폰은_입장권과_사용자_제한을_조회하지_않는다() {
        useCase.requireAdmission(10L, 42L);

        org.mockito.Mockito.verify(room).isEnabled(10L);
        verifyNoMoreInteractions(room);
        assertThatThrownBy(() -> useCase.enter(command)).isInstanceOf(CoreException.class).extracting("errorCode.code").isEqualTo("E4004");
    }

    @Test
    void 행사_쿠폰은_요청_한도와_유효한_입장권을_모두_확인한다() {
        when(room.isEnabled(10L)).thenReturn(true);
        assertThatThrownBy(() -> useCase.requireAdmission(10L, 42L)).isInstanceOf(RetryAfterException.class).extracting("retryAfterSeconds")
            .isEqualTo(1L);

        when(room.allowIssueRequest(10L, 42L)).thenReturn(true);
        when(room.position(10L, 42L)).thenReturn(-1L);
        assertThatThrownBy(() -> useCase.requireAdmission(10L, 42L)).isInstanceOf(CoreException.class).extracting("errorCode.code")
            .isEqualTo("E4005");
        assertThatThrownBy(() -> useCase.status(command)).isInstanceOf(CoreException.class);

        when(room.position(10L, 42L)).thenReturn(0L);
        when(room.admissionsPerSecond()).thenReturn(10);
        useCase.requireAdmission(10L, 42L);
        assertThat(useCase.status(command).admitted()).isTrue();
    }

    @Test
    void 대기_순번을_초당_입장_인원으로_나누어_대기_시간을_알려준다() {
        when(room.isEnabled(10L)).thenReturn(true);
        when(room.enter(10L, 42L)).thenReturn(11L);
        when(room.admissionsPerSecond()).thenReturn(10);

        assertThat(useCase.enter(command)).extracting("position", "estimatedWaitSeconds").containsExactly(11L, 2L);
    }
}
