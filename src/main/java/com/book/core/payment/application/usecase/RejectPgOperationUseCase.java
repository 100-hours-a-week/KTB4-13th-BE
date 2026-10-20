package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RejectPgOperationUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final PreparePgOperationCommand command) {
        lockOrder.execute(command.orderKey());
        operations.findByRequestHash(command.requestHash()).ifPresent(operation -> {
            operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
            if (!operation.completed()) {
                operation.reject();
            }
        });
    }
}
