package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.payment.application.result.CanceledPaymentResult;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.GetPgOperationUseCase;
import com.book.core.payment.application.usecase.RequestPgCancelUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PgOperation;
import com.book.core.point.domain.PointAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PartialCancelPgPaymentUseCase {
    private final GetPgOperationUseCase getOperation;
    private final ValidateCancelUseCase validate;
    private final CalculatePartialCancelUseCase calculate;
    private final GetPaymentUseCase getPayment;
    private final CancelBalanceRepositoryPort balances;
    private final RequestPgCancelUseCase requestCancel;
    private final PartialCancelPaymentUseCase complete;

    @Transactional
    public Long execute(final PartialCancelCommand command) {
        final PgOperation operation = getOperation.execute(command.operation());
        if (operation.completed()) {
            return operation.resultId();
        }
        validate.validatePartial(command.userId(), command.action());
        final CancelCalculated calculated = calculate.execute(command.action());
        PointAmount.requireNonNegativeWhole(calculated.pointAmount());
        final Payment payment = getPayment.execute(command.action().orderKey());
        final CancelBalance balance =
            balances.findByOrderId(payment.orderId()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final CanceledPaymentResult result = requestCancel.execute(command.action().orderKey(), payment, calculated.paidAmount(),
            balance.cancellablePaidAmount(), operation.pgIdempotencyKey());
        final Long cancelId = complete.execute(command.action(), calculated, result);
        operation.complete(cancelId);
        return cancelId;
    }
}
