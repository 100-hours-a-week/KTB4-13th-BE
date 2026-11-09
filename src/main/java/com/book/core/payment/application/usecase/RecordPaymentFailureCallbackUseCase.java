package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordPaymentFailureCallbackUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final ValidatePaymentAttemptUseCase validateAttempt;
    private final ValidatePaymentUseCase validatePayment;
    private final FailPaymentUseCase failPayment;
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;
    private final PgOperationRepositoryPort operations;
    private final ReleaseActivePaymentReservationUseCase releaseReservation;

    @Transactional
    public boolean execute(final Long userId, final String orderKey, final Long attemptId, final String code, final String message) {
        final Order order = lockOrder.execute(orderKey);
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        validateAttempt.execute(orderKey, attemptId);
        validatePayment.validateForFail(order.id());
        failPayment.execute(orderKey, code, message);
        if (!operations.existsPending(orderKey)) {
            final Payment payment = payments.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
            if (payment.paidAmount().signum() == 0) {
                releaseReservation.execute(orderKey);
                return false;
            }
            final PaymentAttempt attempt = attempts.findById(attemptId).orElseThrow(() -> new CoreException(ErrorCode.PG_REVIEW_REQUIRED));
            final String hash = PgOperation.hash("unidentified-payment-attempt:" + attempt.id());
            final PgOperation operation = new PgOperation(orderKey, hash, hash, PgOperationType.APPROVE);
            operation.recordUnidentifiedApprovalDetails(payment.paidAmount(), attempt.pgOrderId(), attempt.id());
            operation.markUncertain(Instant.now());
            operation.requireReview();
            operations.save(operation);
            log.error("[PAYMENT] 실패 콜백의 결제 결과를 확인할 수 없어 운영 확인이 필요합니다.");
        }
        return true;
    }
}
