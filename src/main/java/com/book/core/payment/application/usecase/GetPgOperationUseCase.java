package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPgOperationUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.MANDATORY)
    public PgOperation execute(final PreparePgOperationCommand command) {
        lockOrder.execute(command.orderKey());
        final PgOperation operation =
            operations.findByRequestHash(command.requestHash()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
        operation.verifyRetryWindow();
        return operation;
    }
}
