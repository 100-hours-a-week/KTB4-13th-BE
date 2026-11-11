package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.command.IssuanceRequested;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.port.CouponIssueQueuePort;
import com.book.core.coupon.application.port.CouponIssueQueuePort.Reservation;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.domain.CouponIssuePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnqueueCouponIssueUseCaseTest {
    private final IssueCouponUseCase validation = mock(IssueCouponUseCase.class);
    private final CouponIssueQueuePort queue = mock(CouponIssueQueuePort.class);
    private final EnqueueCouponIssueUseCase useCase = new EnqueueCouponIssueUseCase(validation, queue);
    private final CouponIssueState issueState = mock(CouponIssueState.class);
    private final IssueCouponCommand command = new IssueCouponCommand(42L, 10L);
    private final IssuanceRequested issue = new IssuanceRequested(10L, 42L, 2L);

    @BeforeEach
    void setUp() {
        final var policy = mock(CouponIssuePolicy.class);
        when(policy.maxUseCount()).thenReturn(2L);
        when(validation.validateIssuable(42L, 10L, issueState)).thenReturn(policy);
    }

    @Test
    void 검증된_발급_요청을_예약한다() {
        when(queue.reserve(issue)).thenReturn(Reservation.ACCEPTED);

        useCase.execute(command, issueState);

        verify(queue).reserve(issue);
    }

    @ParameterizedTest
    @CsvSource({"DUPLICATE, COUPON_ALREADY_ISSUED", "SOLD_OUT, COUPON_SOLD_OUT", "FAILED, COUPON_ISSUE_FAILED"})
    void 예약_실패를_발급_오류로_반환한다(final Reservation reservation, final ErrorCode errorCode) {
        when(queue.reserve(issue)).thenReturn(reservation);

        assertThatThrownBy(() -> useCase.execute(command, issueState)).isInstanceOf(CoreException.class).extracting("errorCode")
            .isEqualTo(errorCode);
    }
}
