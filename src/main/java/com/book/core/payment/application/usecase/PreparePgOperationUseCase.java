package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.GetCancelBalanceUseCase;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import com.book.core.point.domain.PointAmount;
import java.math.BigDecimal;
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
    private final PaymentRepositoryPort payments;
    private final CalculatePartialCancelUseCase calculatePartialCancel;
    private final GetCancelBalanceUseCase getCancelBalance;

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
            case CANCEL -> {
                validateCancel.validate(command.userId(), new CancelAction(command.orderKey()));
                final Payment payment = payments.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
                PointAmount.requireNonNegativeWhole(payment.usedPoint());
                final BigDecimal amount = getCancelBalance.execute(order.id()).cancellablePaidAmount();
                validateRefundAmount(amount, amount);
            }
            case PARTIAL_CANCEL -> {
                final PartialCancelAction action = new PartialCancelAction(command.orderKey(), command.orderItemId(), command.quantity());
                validateCancel.validatePartial(command.userId(), action);
                final CancelCalculated calculated = calculatePartialCancel.execute(action);
                PointAmount.requireNonNegativeWhole(calculated.pointAmount());
                validateRefundAmount(calculated.paidAmount(), getCancelBalance.execute(order.id()).cancellablePaidAmount());
            }
        }
        operations.save(new PgOperation(command.orderKey(), command.requestHash(), command.inputHash(), command.type()));
    }

    private static void validateRefundAmount(final BigDecimal amount, final BigDecimal refundableAmount) {
        if (amount == null || refundableAmount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0
            || refundableAmount.signum() < 0 || refundableAmount.stripTrailingZeros().scale() > 0
            || amount.compareTo(refundableAmount) > 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
    }
}
