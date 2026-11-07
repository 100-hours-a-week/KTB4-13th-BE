package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ValidatePaymentAttemptUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final String orderKey, final Long attemptId) {
        final Order order = lockOrder.execute(orderKey);
        final Payment payment = payments.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final PaymentAttempt attempt =
            attempts.findLatestByPaymentId(payment.id()).orElseThrow(() -> new CoreException(ErrorCode.PG_REVIEW_REQUIRED));
        if (attemptId == null || !attempt.id().equals(attemptId) || attempt.state() != PaymentAttemptState.READY
            || !attempt.orderKey().equals(orderKey)) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
    }
}
