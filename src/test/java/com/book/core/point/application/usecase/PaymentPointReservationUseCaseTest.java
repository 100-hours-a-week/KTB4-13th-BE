package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointRecoverySettlementPort;
import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointReservation;
import com.book.core.point.domain.PointReservationState;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class PaymentPointReservationUseCaseTest {
    private final PointBalanceRepositoryPort balances = mock(PointBalanceRepositoryPort.class);
    private final PointReservationRepositoryPort reservations = mock(PointReservationRepositoryPort.class);
    private final PointHistoryRepositoryPort histories = mock(PointHistoryRepositoryPort.class);
    private final PointRecoverySettlementPort settlement = mock(PointRecoverySettlementPort.class);
    private final ObjectProvider<PointRecoverySettlementPort> settlements = mock(ObjectProvider.class);
    private final ReservePaymentPointsUseCase reserve = new ReservePaymentPointsUseCase(balances, reservations);
    private final ConfirmPaymentPointsUseCase confirm = new ConfirmPaymentPointsUseCase(balances, reservations, histories);
    private final ReleasePaymentPointsUseCase release = new ReleasePaymentPointsUseCase(balances, reservations, settlements);
    private final PointBalance balance = new PointBalance(5L, 1L, BigDecimal.valueOf(1000));

    @Test
    void 동일_시도의_예약_재요청은_잔액을_다시_묶지_않는다() {
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        reserve.execute(1L, 7L, BigDecimal.valueOf(600), 9L);
        final ArgumentCaptor<PointReservation> saved = ArgumentCaptor.forClass(PointReservation.class);
        verify(reservations).save(saved.capture());
        when(reservations.findByAttemptId(7L)).thenReturn(Optional.of(saved.getValue()));

        reserve.execute(1L, 7L, BigDecimal.valueOf(600), 9L);

        assertThat(balance.reserved()).isEqualByComparingTo("600");
        assertThat(balance.available()).isEqualByComparingTo("400");
        assertThat(saved.getValue().state()).isEqualTo(PointReservationState.RESERVED);
    }

    @Test
    void 같은_시도에_다른_금액을_요청하면_거절한다() {
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        when(reservations.findByAttemptId(7L)).thenReturn(Optional.of(new PointReservation(1L, 7L, BigDecimal.valueOf(600), 9L)));

        assertThatThrownBy(() -> reserve.execute(1L, 7L, BigDecimal.valueOf(500), 9L)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATE));
        verify(reservations, never()).save(any());
    }

    @Test
    void 다른_결제에_예약된_포인트는_다시_예약할_수_없다() {
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        balance.reserve(BigDecimal.valueOf(700));

        assertThatThrownBy(() -> reserve.execute(1L, 8L, BigDecimal.valueOf(301), 10L)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.POINT_EXCEEDS_BALANCE));
        assertThat(balance.reserved()).isEqualByComparingTo("700");
        verify(reservations, never()).save(any());
    }

    @Test
    void 예약_확정은_한번만_차감하고_결제_이력을_기록한다() {
        final PointReservation reservation = reserved(600);
        balance.reserve(BigDecimal.valueOf(300));

        confirm.execute(1L, 7L);
        confirm.execute(1L, 7L);

        assertThat(balance.balance()).isEqualByComparingTo("400");
        assertThat(balance.reserved()).isEqualByComparingTo("300");
        assertThat(reservation.state()).isEqualTo(PointReservationState.CONFIRMED);
        final ArgumentCaptor<PointHistory> history = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().referenceId()).isEqualTo(9L);
        assertThat(history.getValue().amount()).isEqualByComparingTo("-600");
        assertThat(history.getValue().balanceAfter()).isEqualByComparingTo("400");
    }

    @Test
    void 예약_해제는_한번만_잔액을_복원하고_이미_해제된_예약은_확정할_수_없다() {
        final PointReservation reservation = reserved(600);
        when(settlements.getIfAvailable()).thenReturn(settlement);

        release.execute(1L, 7L);
        release.execute(1L, 7L);

        assertThat(balance.reserved()).isEqualByComparingTo("0");
        assertThat(reservation.state()).isEqualTo(PointReservationState.RELEASED);
        verify(settlement).settleReleasedReservation(balance, BigDecimal.valueOf(600), 7L);
        assertThatThrownBy(() -> confirm.execute(1L, 7L)).isInstanceOf(CoreException.class);
        verifyNoInteractions(histories);
    }

    @Test
    void 영원_예약도_상태는_확정하지만_차감_이력은_남기지_않는다() {
        final PointReservation reservation = reserved(0);

        confirm.execute(1L, 7L);

        assertThat(reservation.state()).isEqualTo(PointReservationState.CONFIRMED);
        assertThat(balance.balance()).isEqualByComparingTo("1000");
        verifyNoInteractions(histories, settlement);
    }

    @Test
    void 다른_회원의_예약은_확정하거나_해제하지_않는다() {
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        final PointReservation reservation = new PointReservation(2L, 7L, BigDecimal.valueOf(100), 9L);
        when(reservations.findByAttemptId(7L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> confirm.execute(1L, 7L)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> release.execute(1L, 7L)).isInstanceOf(CoreException.class);
        assertThat(reservation.state()).isEqualTo(PointReservationState.RESERVED);
        verifyNoInteractions(histories, settlement);
    }

    @Test
    void 잘못된_금액과_없는_예약을_거절한다() {
        assertThatThrownBy(() -> reserve.execute(1L, 7L, BigDecimal.valueOf(-1), 9L)).isInstanceOfSatisfying(CoreException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        assertThatThrownBy(() -> confirm.execute(1L, 7L)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> release.execute(1L, 7L)).isInstanceOf(CoreException.class);
    }

    private PointReservation reserved(final int amount) {
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        balance.reserve(BigDecimal.valueOf(amount));
        final PointReservation reservation = new PointReservation(1L, 7L, BigDecimal.valueOf(amount), 9L);
        when(reservations.findByAttemptId(7L)).thenReturn(Optional.of(reservation));
        return reservation;
    }
}
