package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.domain.PgOperation;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Confirms an explicitly requested compensation before releasing its point reservation. */
@UseCase
@RequiredArgsConstructor
public class CompensatePgApprovalUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;
    private final ValidatePaymentAttemptUseCase validateAttempt;
    private final ReleaseActivePaymentReservationUseCase releaseReservation;
    private final PaymentClient client;

    @Transactional
    public void execute(final String orderKey, final String requestHash) {
        lockOrder.execute(orderKey);
        final PgOperation operation =
            operations.findByRequestHash(requestHash).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (!orderKey.equals(operation.orderKey())) {
            throw new CoreException(ErrorCode.PG_REQUEST_CONFLICT);
        }
        if (operation.rejected()) {
            return;
        }
        if (operation.reviewRequired() || operation.completed() || !operation.compensationRequested()
            || operation.compensationIdempotencyKey() == null) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        validateAttempt.execute(orderKey, operation.attemptId());
        // NOTE(케빈): 주문 잠금 중 PG 호출의 대기 시간과 승인·취소 경합을 함께 검토해 주세요.
        PgPaymentResult result = client.find(operation.externalPaymentKey());
        verifyIdentity(operation, result);
        if ("DONE".equals(result.status()) && operation.approvalAmount().compareTo(result.balanceAmount()) == 0) {
            result = client.cancel(operation.externalPaymentKey(), operation.approvalAmount(), operation.compensationIdempotencyKey());
            verifyIdentity(operation, result);
        }
        if (!"CANCELED".equals(result.status()) || result.balanceAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
        releaseReservation.execute(orderKey);
        operation.reject();
    }

    private void verifyIdentity(final PgOperation operation, final PgPaymentResult result) {
        if (result == null || !operation.pgOrderId().equals(result.orderKey())
            || !operation.externalPaymentKey().equals(result.paymentKey()) || result.totalAmount() == null
            || operation.approvalAmount().compareTo(result.totalAmount()) != 0 || result.balanceAmount() == null) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
    }
}
