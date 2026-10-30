package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointRecoverySettlementPort;
import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointReservation;
import com.book.core.point.domain.PointReservationState;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

@UseCase
@RequiredArgsConstructor
public class ReleasePaymentPointsUseCase {
    private final PointBalanceRepositoryPort balances;
    private final PointReservationRepositoryPort reservations;
    private final ObjectProvider<PointRecoverySettlementPort> recoverySettlement;

    @Transactional
    public void execute(final Long userId, final Long attemptId) {
        final PointBalance balance = balances.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final PointReservation reservation =
            reservations.findByAttemptId(attemptId).orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_INVALID_STATE));
        if (!reservation.userId().equals(userId)) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (reservation.state() == PointReservationState.RELEASED) {
            return;
        }
        if (reservation.state() != PointReservationState.RESERVED) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        balance.release(reservation.amount());
        reservation.release();
        if (reservation.amount().signum() > 0) {
            final PointRecoverySettlementPort settlement = recoverySettlement.getIfAvailable();
            if (settlement != null) {
                settlement.settleReleasedReservation(balance, reservation.amount(), attemptId);
            }
        }
    }
}
