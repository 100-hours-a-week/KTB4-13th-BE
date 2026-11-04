package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.PointReservation;
import com.book.core.point.domain.PointReservationState;
import com.book.core.point.domain.enums.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ConfirmPaymentPointsUseCase {
    private final PointBalanceRepositoryPort balances;
    private final PointReservationRepositoryPort reservations;
    private final PointHistoryRepositoryPort histories;

    @Transactional
    public void execute(final Long userId, final Long attemptId) {
        final PointBalance balance = balances.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final PointReservation reservation =
            reservations.findByAttemptId(attemptId).orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_INVALID_STATE));
        if (!reservation.userId().equals(userId)) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        if (reservation.state() == PointReservationState.CONFIRMED) {
            return;
        }
        if (reservation.state() != PointReservationState.RESERVED) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        balance.confirm(reservation.amount());
        reservation.confirm();
        if (reservation.amount().signum() > 0) {
            histories.save(PointHistory.builder().userId(userId).type(PointType.PAYMENT).referenceId(reservation.paymentId())
                .amount(reservation.amount().negate()).balanceAfter(balance.balance()).build());
        }
    }
}
