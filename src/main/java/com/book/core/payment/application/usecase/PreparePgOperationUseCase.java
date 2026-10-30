package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CalculatePartialCancelUseCase;
import com.book.core.cancel.application.usecase.GetCancelBalanceUseCase;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class PreparePgOperationUseCase {
    private final LockPaymentOrderUseCase lockOrder;
    private final PgOperationRepositoryPort operations;
    private final ValidatePaymentUseCase validatePayment;
    private final ValidatePaymentAttemptUseCase validateAttempt;
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;
    private final com.book.core.cancel.application.usecase.ValidateCancelUseCase validateCancel;
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
            final PgOperation operation = existing.get();
            if (command.type() == PgOperationType.APPROVE && command.amount().signum() > 0
                && (!operation.orderKey().equals(command.orderKey()) || !operation.inputHash().equals(command.inputHash())
                    || operation.type() != command.type())) {
                saveIsolatedReview(command);
                return;
            }
            operation.verifyInput(command.orderKey(), command.inputHash(), command.type());
            final boolean currentApproval = command.type() == PgOperationType.APPROVE && isCurrentAttempt(order, command);
            final boolean startedLate = order.expiresAt() != null
                && !LocalDateTime.ofInstant(operation.requestedAt(), ZoneId.systemDefault()).isBefore(order.expiresAt());
            if (!operation.completed() && command.type() == PgOperationType.APPROVE && (!currentApproval || startedLate)) {
                if (command.amount().signum() == 0) {
                    operation.reject();
                } else {
                    operation.markUncertain(Instant.now());
                    if (!currentApproval) {
                        operation.isolateFromOrder();
                    }
                    if (operation.requireReview()) {
                        log.error("[PAYMENT] 주문 기한 후 시작한 결제 작업의 운영 확인이 필요합니다.");
                    }
                }
                return;
            }
            operation.verifyRetryWindow();
            return;
        }
        final boolean currentApproval = command.type() == PgOperationType.APPROVE && isCurrentAttempt(order, command);
        final boolean expired = order.expiresAt() != null && !order.expiresAt().isAfter(LocalDateTime.now());
        if (currentApproval && command.amount().signum() > 0) {
            final String unidentifiedHash = PgOperation.hash("unidentified-payment-attempt:" + command.attemptId());
            final Optional<PgOperation> unidentified = operations.findByRequestHash(unidentifiedHash);
            if (unidentified.isPresent() && unidentified.get().linkUnidentifiedApproval(command.orderKey(), command.attemptId(),
                command.pgOrderId(), command.amount(), command.externalPaymentKey(), command.requestHash(), command.inputHash())) {
                return;
            }
            if (unidentified.isPresent() || operations.existsPending(command.orderKey())) {
                saveIsolatedReview(command);
                return;
            }
        }
        if (command.type() == PgOperationType.APPROVE && (!currentApproval || expired)) {
            if (command.amount().signum() == 0) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            if (!currentApproval) {
                saveIsolatedReview(command);
                return;
            }
            // PG could have approved before this late callback: keep the operation pending for review.
            final PgOperation late = new PgOperation(command.orderKey(), command.requestHash(), command.inputHash(), command.type());
            late.recordApprovalDetails(command.externalPaymentKey(), command.amount(), command.pgOrderId(), command.attemptId());
            late.markUncertain(Instant.now());
            late.requireReview();
            operations.save(late);
            log.error("[PAYMENT] 주문 기한 후 도착한 결제 결과의 운영 확인이 필요합니다.");
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
                validateAttempt.execute(command.orderKey(), command.attemptId());
            }
            case CANCEL -> {
                validateCancel.validate(command.userId(), new com.book.core.cancel.domain.CancelAction(command.orderKey()));
                final Payment payment = payments.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
                requireNonNegativeWholePoint(payment.usedPoint());
                final BigDecimal amount = getCancelBalance.execute(order.id()).cancellablePaidAmount();
                validateRefundAmount(amount, amount);
            }
            case PARTIAL_CANCEL -> {
                final PartialCancelAction action = new PartialCancelAction(command.orderKey(), command.orderItemId(), command.quantity());
                validateCancel.validatePartial(command.userId(), action);
                final CancelCalculated calculated = calculatePartialCancel.execute(action);
                requireNonNegativeWholePoint(calculated.pointAmount());
                validateRefundAmount(calculated.paidAmount(), getCancelBalance.execute(order.id()).cancellablePaidAmount());
            }
        }
        final PgOperation operation = new PgOperation(command.orderKey(), command.requestHash(), command.inputHash(), command.type());
        if (command.type() == PgOperationType.APPROVE) {
            operation.recordApprovalDetails(command.externalPaymentKey(), command.amount(), command.pgOrderId(), command.attemptId());
        }
        operations.save(operation);
    }

    private void saveIsolatedReview(final PreparePgOperationCommand command) {
        final String isolatedHash = command.isolatedApprovalHash();
        final Optional<PgOperation> existing = operations.findByRequestHash(isolatedHash);
        if (existing.isPresent()) {
            existing.get().verifyInput(command.orderKey(), command.inputHash(), command.type());
            return;
        }
        final PgOperation anomaly = new PgOperation(command.orderKey(), isolatedHash, command.inputHash(), command.type());
        anomaly.recordApprovalDetails(command.externalPaymentKey(), command.amount(), command.pgOrderId(), command.attemptId());
        anomaly.markUncertain(Instant.now());
        anomaly.isolateFromOrder();
        anomaly.requireReview();
        operations.save(anomaly);
        log.error("[PAYMENT] 결제 확인 요청이 기존 작업과 일치하지 않아 운영 확인이 필요합니다.");
    }

    private static void validateRefundAmount(final BigDecimal amount, final BigDecimal refundableAmount) {
        if (amount == null || refundableAmount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0
            || refundableAmount.signum() < 0 || refundableAmount.stripTrailingZeros().scale() > 0
            || amount.compareTo(refundableAmount) > 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
    }

    private static void requireNonNegativeWholePoint(final BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean isCurrentAttempt(final Order order, final PreparePgOperationCommand command) {
        if (order.status() != com.book.core.order.domain.OrderStatus.CREATED || command.attemptId() == null) {
            return false;
        }
        final Optional<Payment> payment = payments.findByOrderId(order.id());
        if (payment.isEmpty() || payment.get().state() != PaymentState.READY
            || payment.get().paidAmount().compareTo(command.amount()) != 0) {
            return false;
        }
        final Optional<PaymentAttempt> latest = attempts.findLatestByPaymentId(payment.get().id());
        return latest.isPresent() && latest.get().id().equals(command.attemptId()) && latest.get().state() == PaymentAttemptState.READY
            && latest.get().orderKey().equals(command.orderKey()) && latest.get().pgOrderId().equals(command.pgOrderId());
    }
}
