package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
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
        PgOperation operation = operations.findByRequestHash(command.requestHash()).orElse(null);
        if (command.type() == PgOperationType.APPROVE && (operation == null || !operation.orderKey().equals(command.orderKey())
            || operation.type() != command.type() || !operation.inputHash().equals(command.inputHash()))) {
            operation = operations.findByRequestHash(command.isolatedApprovalHash()).orElse(operation);
        }
        if (operation == null) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
        operation.verifyRetryWindow();
        return operation;
    }
}
