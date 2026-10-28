package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PreparePgOperationUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;
    private final ValidatePaymentUseCase validatePayment;
    private final com.book.core.cancel.application.usecase.ValidateCancelUseCase validateCancel;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final PreparePgOperationCommand command) {
        final Order order = lockOrder.execute(command.orderKey());
        if (command.type() != PgOperationType.APPROVE && !order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        final Optional<PgOperation> existing = operations.findByRequestHash(command.requestHash());
        if (existing.isPresent()) {
            existing.get().verifyInput(command.orderKey(), command.inputHash(), command.type());
            existing.get().verifyRetryWindow();
            return;
        }
        if (operations.existsPending(command.orderKey())) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
        switch (command.type()) {
            case APPROVE -> {
                if (order.status() != com.book.core.order.domain.OrderStatus.CREATED) {
                    throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
                }
                validatePayment.validateForApprove(order.id(), command.amount());
            }
            case CANCEL -> validateCancel.validate(command.userId(), new com.book.core.cancel.domain.CancelAction(command.orderKey()));
            case PARTIAL_CANCEL -> validateCancel.validatePartial(command.userId(),
                new com.book.core.cancel.domain.PartialCancelAction(command.orderKey(), command.orderItemId(), command.quantity()));
        }
        operations.save(new PgOperation(command.orderKey(), command.requestHash(), command.inputHash(), command.type()));
    }
}
