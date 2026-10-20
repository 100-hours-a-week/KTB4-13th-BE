package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.payment.application.result.CanceledPaymentResult;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.GetPgOperationUseCase;
import com.book.core.payment.application.usecase.RequestPgCancelUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PgOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CancelPgPaymentUseCase {
    private final GetPgOperationUseCase getOperation;
    private final ValidateCancelUseCase validate;
    private final GetPaymentUseCase getPayment;
    private final CancelBalanceRepositoryPort balances;
    private final RequestPgCancelUseCase requestCancel;
    private final CancelPaymentUseCase complete;

    @Transactional
    public Long execute(final CancelCommand command) {
        final PgOperation operation = getOperation.execute(command.operation());
        if (operation.completed()) {
            return operation.resultId();
        }
        validate.validate(command.userId(), command.action());
        final Payment payment = getPayment.execute(command.action().orderKey());
        final CancelBalance balance =
            balances.findByOrderId(payment.orderId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final CanceledPaymentResult result = requestCancel.execute(command.action().orderKey(), payment, balance.cancellablePaidAmount(),
            balance.cancellablePaidAmount(), operation.pgIdempotencyKey());
        final Long cancelId = complete.execute(command.action(), result);
        operation.complete(cancelId);
        return cancelId;
    }
}
