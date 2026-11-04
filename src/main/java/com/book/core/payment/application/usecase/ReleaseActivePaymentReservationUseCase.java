package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.point.application.usecase.ReleasePaymentPointsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReleaseActivePaymentReservationUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;
    private final ReleasePaymentPointsUseCase releasePoints;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final String orderKey) {
        final Order order = lockOrder.execute(orderKey);
        payments.findByOrderId(order.id()).flatMap(payment -> attempts.findLatestByPaymentId(payment.id())).ifPresent(attempt -> {
            if (attempt.state() == PaymentAttemptState.READY) {
                attempt.fail();
                releasePoints.execute(order.userId(), attempt.id());
            }
        });
    }
}
