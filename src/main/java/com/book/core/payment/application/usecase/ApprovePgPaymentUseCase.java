package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.port.PaymentClient;
import com.book.core.payment.application.result.ApprovedPaymentResult;
import com.book.core.payment.application.result.PgPaymentResult;
import com.book.core.payment.application.result.PaymentCompletionResult;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ApprovePgPaymentUseCase {
    private final GetPgOperationUseCase getOperation;
    private final GetPaymentOrderUseCase getOrder;
    private final ValidatePaymentUseCase validate;
    private final ValidatePaymentAttemptUseCase validateAttempt;
    private final PaymentClient client;
    private final CompletePaymentUseCase complete;

    @Transactional
    public PaymentCompletionResult execute(final ApprovePaymentCommand command) {
        final PgOperation operation = getOperation.execute(command.operation());
        if (operation.completed()) {
            return new PaymentCompletionResult(operation.resultId(), false);
        }
        if (operation.reviewRequired()) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        if (operation.compensationRequested()) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
        validateAttempt.execute(command.orderKey(), command.attemptId());
        final Order order = getOrder.execute(command.orderKey(), OrderStatus.CREATED);
        validate.validateForApprove(order.id(), command.amount());
        ApprovedPaymentResult approval = new ApprovedPaymentResult("", null, null);
        if (command.amount().compareTo(BigDecimal.ZERO) > 0) {
            PgPaymentResult result = client.find(command.externalPaymentKey());
            verify(command, result);
            TossPaymentStatus status = TossPaymentStatus.from(result.status());
            if (status == TossPaymentStatus.ABORTED || status == TossPaymentStatus.EXPIRED) {
                throw new CoreException(ErrorCode.PG_REJECTED);
            }
            if (status == TossPaymentStatus.READY) {
                throw new CoreException(ErrorCode.PG_PENDING);
            }
            if (status == TossPaymentStatus.IN_PROGRESS) {
                if (order.expiresAt() != null && !order.expiresAt().isAfter(LocalDateTime.now())) {
                    throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
                }
                result = client.approve(command.externalPaymentKey(), command.pgOrderId(), command.amount(), operation.pgIdempotencyKey());
                verify(command, result);
                status = TossPaymentStatus.from(result.status());
            }
            if (status != TossPaymentStatus.DONE || result.balanceAmount().compareTo(command.amount()) != 0 || result.method() == null
                || (result.method() == PaymentMethod.CARD && (result.approveCode() == null || result.approveCode().isBlank()))) {
                throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
            }
            approval = new ApprovedPaymentResult(result.paymentKey(), result.method(), result.approveCode());
        }
        final Long paymentId = complete.execute(command.orderKey(), command.attemptId(), approval);
        operation.complete(paymentId);
        return new PaymentCompletionResult(paymentId, true);
    }

    private void verify(final ApprovePaymentCommand command, final PgPaymentResult result) {
        if (result == null || !command.pgOrderId().equals(result.orderKey()) || !command.externalPaymentKey().equals(result.paymentKey())
            || result.totalAmount() == null || result.totalAmount().compareTo(command.amount()) != 0 || result.balanceAmount() == null) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
    }

    private enum TossPaymentStatus {
        READY,
        IN_PROGRESS,
        WAITING_FOR_DEPOSIT,
        DONE,
        CANCELED,
        PARTIAL_CANCELED,
        ABORTED,
        EXPIRED,
        UNKNOWN;

        private static TossPaymentStatus from(final String status) {
            if (status == null) {
                return UNKNOWN;
            }
            try {
                return valueOf(status);
            } catch (final IllegalArgumentException exception) {
                return UNKNOWN;
            }
        }
    }
}
