package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.payment.domain.PgOperation;
import com.book.core.point.application.usecase.ReleasePaymentPointsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FailPaymentAttemptUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;
    private final PaymentAttemptRepositoryPort attempts;
    private final ReleasePaymentPointsUseCase releasePoints;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final PreparePgOperationCommand command) {
        final Order order = lockOrder.execute(command.orderKey());
        final PgOperation operation =
            operations.findByRequestHash(command.requestHash()).orElseThrow(() -> new CoreException(ErrorCode.PG_REVIEW_REQUIRED));
        operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
        if (operation.completed()) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        final PaymentAttempt attempt =
            attempts.findById(command.attemptId()).orElseThrow(() -> new CoreException(ErrorCode.PG_REVIEW_REQUIRED));
        if (!attempt.orderKey().equals(command.orderKey())) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        if (attempt.state() == PaymentAttemptState.COMPLETED) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        if (attempt.state() == PaymentAttemptState.READY) {
            attempt.fail();
            releasePoints.execute(order.userId(), attempt.id());
        }
        operation.reject();
    }
}
