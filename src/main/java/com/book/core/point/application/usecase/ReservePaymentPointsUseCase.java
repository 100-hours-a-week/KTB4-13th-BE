package com.book.core.point.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointReservationRepositoryPort;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointReservation;
import com.book.core.point.domain.PointReservationState;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReservePaymentPointsUseCase {
    private final PointBalanceRepositoryPort balances;
    private final PointReservationRepositoryPort reservations;

    @Transactional
    public void execute(final Long userId, final Long attemptId, final BigDecimal amount, final Long paymentId) {
        PointAmount.requireWhole(amount);
        if (userId == null || attemptId == null || paymentId == null || amount == null || amount.signum() < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final PointBalance balance = balances.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var existing = reservations.findByAttemptId(attemptId);
        if (existing.isPresent()) {
            final PointReservation reservation = existing.get();
            if (!reservation.userId().equals(userId) || !reservation.paymentId().equals(paymentId)
                || reservation.amount().compareTo(amount) != 0 || reservation.state() != PointReservationState.RESERVED) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            return;
        }
        balance.reserve(amount);
        reservations.save(new PointReservation(userId, attemptId, amount, paymentId));
    }
}
