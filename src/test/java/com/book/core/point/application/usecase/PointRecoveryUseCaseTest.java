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
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointRecoveryRepositoryPort;
import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.PointRecoverySettlement;
import com.book.core.point.domain.PointReservation;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PointRecoveryUseCaseTest {
    private final PointBalanceRepositoryPort balances = mock(PointBalanceRepositoryPort.class);
    private final PointRecoveryRepositoryPort recoveries = mock(PointRecoveryRepositoryPort.class);
    private final PointHistoryRepositoryPort histories = mock(PointHistoryRepositoryPort.class);
    private final PointReservationRepositoryPort reservations = mock(PointReservationRepositoryPort.class);
    private final ReclaimPaymentBonusUseCase reclaim = new ReclaimPaymentBonusUseCase(balances, recoveries, histories);
    private final SettlePointRecoveriesUseCase settle = new SettlePointRecoveriesUseCase(recoveries, histories);
    private final ReleasePaymentPointsUseCase release = new ReleasePaymentPointsUseCase(balances, reservations, settle);
    private final RestorePaymentPointsUseCase restore = new RestorePaymentPointsUseCase(balances, histories);
    private final DeductPointUseCase deduct = new DeductPointUseCase(balances, histories, recoveries);

    @Test
    void 보너스_회수는_다른_결제의_예약을_보호하고_부족분을_기록한다() {
        final PointBalance balance = balance(1000);
        balance.reserve(BigDecimal.valueOf(800));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));

        reclaim.execute(1L, 21L, BigDecimal.valueOf(500));

        final ArgumentCaptor<PointRecovery> recovery = ArgumentCaptor.forClass(PointRecovery.class);
        verify(recoveries).save(recovery.capture());
        assertThat(recovery.getValue().originalAmount()).isEqualByComparingTo("500");
        assertThat(recovery.getValue().remainingAmount()).isEqualByComparingTo("300");
        assertThat(balance.balance()).isEqualByComparingTo("800");
        assertThat(balance.reserved()).isEqualByComparingTo("800");
        assertThat(balance.available()).isEqualByComparingTo("0");
        final ArgumentCaptor<PointHistory> history = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().amount()).isEqualByComparingTo("-200");
        assertThat(history.getValue().balanceAfter()).isEqualByComparingTo("800");
    }

    @Test
    void 같은_회수의_재요청은_중복_차감하지_않고_금액이_다르면_거절한다() {
        final PointBalance balance = balance(1000);
        final PointRecovery existing = new PointRecovery(1L, PointType.PAYMENT, 21L, BigDecimal.valueOf(500), BigDecimal.valueOf(300));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        when(recoveries.findBySource(PointType.PAYMENT, 21L)).thenReturn(Optional.of(existing));

        reclaim.execute(1L, 21L, BigDecimal.valueOf(500));
        assertThatThrownBy(() -> reclaim.execute(1L, 21L, BigDecimal.valueOf(600))).isInstanceOf(CoreException.class);

        assertThat(balance.balance()).isEqualByComparingTo("1000");
        verify(recoveries, never()).save(any());
        verifyNoInteractions(histories);
    }

    @Test
    void 예약_해제액은_오래된_미회수부터_상계하고_남은_금액만_사용_가능해진다() {
        final PointBalance balance = balance(1000);
        balance.reserve(BigDecimal.valueOf(1000));
        final PointReservation reservation = new PointReservation(1L, 7L, BigDecimal.valueOf(1000), 9L);
        final PointRecovery first = recovery(11L, 21L, 300);
        final PointRecovery second = recovery(12L, 22L, 400);
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        when(reservations.findByAttemptId(7L)).thenReturn(Optional.of(reservation));
        when(recoveries.findOutstandingByUserId(1L)).thenReturn(List.of(first, second));

        release.execute(1L, 7L);
        release.execute(1L, 7L);

        assertThat(first.remainingAmount()).isEqualByComparingTo("0");
        assertThat(second.remainingAmount()).isEqualByComparingTo("0");
        assertThat(balance.available()).isEqualByComparingTo("300");
        final ArgumentCaptor<PointRecoverySettlement> settlements = ArgumentCaptor.forClass(PointRecoverySettlement.class);
        verify(recoveries, org.mockito.Mockito.times(2)).saveSettlement(settlements.capture());
        assertThat(settlements.getAllValues()).extracting(PointRecoverySettlement::recoveryId).containsExactly(11L, 12L);
        assertThat(settlements.getAllValues()).extracting(PointRecoverySettlement::sourceReferenceId).containsExactly(7L, 7L);
    }

    @Test
    void 적립액이_부족하면_첫_미회수_건만_부분_상계한다() {
        final PointBalance balance = balance(700);
        balance.reserve(BigDecimal.valueOf(500));
        final PointRecovery first = recovery(11L, 21L, 300);
        final PointRecovery second = recovery(12L, 22L, 400);
        when(recoveries.findOutstandingByUserId(1L)).thenReturn(List.of(first, second));

        settle.execute(balance, BigDecimal.valueOf(200), "EARN", 3L);

        assertThat(first.remainingAmount()).isEqualByComparingTo("100");
        assertThat(second.remainingAmount()).isEqualByComparingTo("400");
        assertThat(balance.available()).isEqualByComparingTo("0");
        verify(recoveries).saveSettlement(any());
        final ArgumentCaptor<PointHistory> history = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().balanceAfter()).isEqualByComparingTo("500");
    }

    @Test
    void 취소_복원은_현재_미회수_상계와_별도로_이력을_남긴다() {
        final PointBalance balance = balance(100);
        balance.reserve(BigDecimal.valueOf(80));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));

        restore.execute(1L, 21L, BigDecimal.valueOf(300));

        assertThat(balance.available()).isEqualByComparingTo("320");
        final ArgumentCaptor<PointHistory> history = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().amount()).isEqualByComparingTo("300");
        assertThat(history.getValue().balanceAfter()).isEqualByComparingTo("400");
        verifyNoInteractions(recoveries);
    }

    @Test
    void 영원은_회수와_복원_이력을_만들지_않고_음수_요청은_거절한다() {
        reclaim.execute(1L, 21L, BigDecimal.ZERO);
        restore.execute(1L, 21L, BigDecimal.ZERO);
        assertThatThrownBy(() -> reclaim.execute(1L, 21L, BigDecimal.valueOf(-1))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> restore.execute(1L, 21L, BigDecimal.valueOf(-1))).isInstanceOf(CoreException.class);
        verifyNoInteractions(balances, recoveries, histories);
    }

    @Test
    void 리뷰_회수는_다른_결제의_예약을_보호하고_부족분을_리뷰_사유로_남긴다() {
        final PointBalance balance = balance(1000);
        balance.reserve(BigDecimal.valueOf(800));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));

        deduct.execute(1L, PointType.REVIEW, 21L, BigDecimal.valueOf(500));

        final ArgumentCaptor<PointRecovery> recovery = ArgumentCaptor.forClass(PointRecovery.class);
        verify(recoveries).save(recovery.capture());
        assertThat(recovery.getValue().sourceType()).isEqualTo(PointType.REVIEW);
        assertThat(recovery.getValue().sourceReferenceId()).isEqualTo(21L);
        assertThat(recovery.getValue().remainingAmount()).isEqualByComparingTo("300");
        assertThat(balance.balance()).isEqualByComparingTo("800");
        assertThat(balance.reserved()).isEqualByComparingTo("800");
        final ArgumentCaptor<PointHistory> history = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().type()).isEqualTo(PointType.REVIEW);
        assertThat(history.getValue().referenceId()).isEqualTo(21L);
        assertThat(history.getValue().amount()).isEqualByComparingTo("-200");
    }

    @Test
    void 같은_리뷰_회수_재요청은_중복_차감하지_않고_금액이_다르면_거절한다() {
        final PointBalance balance = balance(1000);
        final PointRecovery existing = new PointRecovery(1L, PointType.REVIEW, 21L, BigDecimal.valueOf(500), BigDecimal.valueOf(300));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));
        when(recoveries.findBySource(PointType.REVIEW, 21L)).thenReturn(Optional.of(existing));

        deduct.execute(1L, PointType.REVIEW, 21L, BigDecimal.valueOf(500));
        assertThatThrownBy(() -> deduct.execute(1L, PointType.REVIEW, 21L, BigDecimal.valueOf(600))).isInstanceOf(CoreException.class);

        assertThat(balance.balance()).isEqualByComparingTo("1000");
        verify(recoveries, never()).save(any());
        verifyNoInteractions(histories);
    }

    @Test
    void 같은_번호의_결제와_리뷰_미회수는_각각_보존하고_발생순으로_상계한다() {
        final PointBalance balance = balance(1000);
        balance.reserve(BigDecimal.valueOf(1000));
        final PointRecovery payment = new PointRecovery(1L, PointType.PAYMENT, 21L, BigDecimal.valueOf(300), BigDecimal.valueOf(300));
        final PointRecovery review = new PointRecovery(1L, PointType.REVIEW, 21L, BigDecimal.valueOf(400), BigDecimal.valueOf(400));
        ReflectionTestUtils.setField(payment, "id", 11L);
        ReflectionTestUtils.setField(review, "id", 12L);
        when(recoveries.findOutstandingByUserId(1L)).thenReturn(List.of(payment, review));

        balance.release(BigDecimal.valueOf(500));
        settle.execute(balance, BigDecimal.valueOf(500), "RELEASE", 9L);

        assertThat(payment.remainingAmount()).isEqualByComparingTo("0");
        assertThat(review.remainingAmount()).isEqualByComparingTo("200");
        assertThat(balance.reserved()).isEqualByComparingTo("500");
        final ArgumentCaptor<PointHistory> historiesCaptured = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories, org.mockito.Mockito.times(2)).save(historiesCaptured.capture());
        assertThat(historiesCaptured.getAllValues()).extracting(PointHistory::type).containsExactly(PointType.PAYMENT, PointType.REVIEW);
        assertThat(historiesCaptured.getAllValues()).extracting(PointHistory::referenceId).containsExactly(21L, 21L);
    }

    @Test
    void 그밖의_포인트_차감은_예약된_포인트를_침범하지_않는다() {
        final PointBalance balance = balance(1000);
        balance.reserve(BigDecimal.valueOf(800));
        when(balances.findByUserIdForUpdate(1L)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> deduct.execute(1L, PointType.PAYMENT, 21L, BigDecimal.valueOf(500))).isInstanceOf(CoreException.class);
        assertThat(balance.balance()).isEqualByComparingTo("1000");
        assertThat(balance.reserved()).isEqualByComparingTo("800");
        verifyNoInteractions(histories, recoveries);
    }

    private PointBalance balance(final int amount) {
        return new PointBalance(5L, 1L, BigDecimal.valueOf(amount));
    }

    private PointRecovery recovery(final Long id, final Long paymentId, final int amount) {
        final PointRecovery recovery =
            new PointRecovery(1L, PointType.PAYMENT, paymentId, BigDecimal.valueOf(amount), BigDecimal.valueOf(amount));
        ReflectionTestUtils.setField(recovery, "id", id);
        return recovery;
    }
}
