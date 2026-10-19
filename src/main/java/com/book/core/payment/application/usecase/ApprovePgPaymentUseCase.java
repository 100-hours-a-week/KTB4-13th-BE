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
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ApprovePgPaymentUseCase {
    private final GetPgOperationUseCase getOperation;
    private final GetPaymentOrderUseCase getOrder;
    private final ValidatePaymentUseCase validate;
    private final PaymentClient client;
    private final CompletePaymentUseCase complete;

    @Transactional
    public PaymentCompletionResult execute(final ApprovePaymentCommand command) {
        final PgOperation operation = getOperation.execute(command.operation());
        if (operation.completed()) {
            return new PaymentCompletionResult(operation.resultId(), false);
        }
        final Order order = getOrder.execute(command.orderKey(), OrderStatus.CREATED);
        validate.validateForApprove(order.id(), command.amount());
        ApprovedPaymentResult approval = new ApprovedPaymentResult("", null, null);
        if (command.amount().compareTo(BigDecimal.ZERO) > 0) {
            PgPaymentResult result = client.find(command.externalPaymentKey());
            verify(command, result);
            if ("ABORTED".equals(result.status()) || "EXPIRED".equals(result.status()) || "READY".equals(result.status())) {
                throw new CoreException(ErrorCode.PG_REJECTED);
            }
            if ("IN_PROGRESS".equals(result.status())) {
                if (result.method() == null) {
                    throw new CoreException(ErrorCode.PG_REJECTED);
                }
                result = client.approve(command.externalPaymentKey(), command.orderKey(), command.amount(), operation.pgIdempotencyKey());
                verify(command, result);
            }
            if (!"DONE".equals(result.status()) || result.balanceAmount().compareTo(command.amount()) != 0 || result.method() == null
                || result.approveCode() == null || result.approveCode().isBlank()) {
                throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
            }
            approval = new ApprovedPaymentResult(result.paymentKey(), result.method(), result.approveCode());
        }
        final Long paymentId = complete.execute(command.orderKey(), approval);
        operation.complete(paymentId);
        return new PaymentCompletionResult(paymentId, true);
    }

    private void verify(final ApprovePaymentCommand command, final PgPaymentResult result) {
        if (result == null || !command.orderKey().equals(result.orderKey()) || !command.externalPaymentKey().equals(result.paymentKey())
            || result.totalAmount() == null || result.totalAmount().compareTo(command.amount()) != 0 || result.balanceAmount() == null) {
            throw new CoreException(ErrorCode.PG_RESPONSE_MISMATCH);
        }
    }
}
