package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.order.domain.Order;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Resumes an approval only after an explicit review decision. */
@UseCase
@RequiredArgsConstructor
public class ResumePgApprovalRecoveryUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;
    private final ValidatePaymentAttemptUseCase validateAttempt;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final String orderKey, final String requestHash) {
        final Order order = lockOrder.execute(orderKey);
        final PgOperation operation =
            operations.findByRequestHash(requestHash).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (!orderKey.equals(operation.orderKey())) {
            throw new CoreException(ErrorCode.PG_REQUEST_CONFLICT);
        }
        if (order.expiresAt() != null
            && !LocalDateTime.ofInstant(operation.requestedAt(), ZoneId.systemDefault()).isBefore(order.expiresAt())) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        validateAttempt.execute(orderKey, operation.attemptId());
        operation.resumeApprovalAfterReview();
    }
}
