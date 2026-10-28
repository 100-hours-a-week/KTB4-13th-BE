package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.LockPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentAttemptRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.application.result.PaymentCreationResult;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentAttemptState;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.usecase.ReservePaymentPointsUseCase;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreatePaymentUseCase {
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptRepositoryPort attempts;
    private final LockPaymentOrderUseCase lockOrder;
    private final ReservePaymentPointsUseCase reservePoints;
    private final PgOperationRepositoryPort operations;

    @Transactional
    public Optional<PaymentCreationResult> findExisting(final String orderKey, final Long userId, final String requestHash,
        final String inputHash) {
        final Order order = lockAndValidate(orderKey, userId);
        return existingReady(order.id(), orderKey, requestHash, inputHash);
    }

    @Transactional
    public PaymentCreationResult execute(final Order order, final Long payerUserId, final PaymentDiscount discount,
        final String requestHash, final String inputHash) {
        final Order locked = lockAndValidate(order.key(), order.userId());
        final Optional<PaymentCreationResult> existing = existingReady(locked.id(), locked.key(), requestHash, inputHash);
        if (existing.isPresent()) {
            return existing.get();
        }
        final Optional<Payment> previous = payments.findByOrderId(locked.id());
        final Payment payment;
        if (previous.isPresent()) {
            payment = previous.get();
            payment.prepareRetry(payerUserId, discount);
        } else {
            payment = payments.save(new Payment(locked.userId(), locked.id(), locked.totalPrice(), discount.useOwnedCouponId(),
                discount.couponDiscount(), discount.usePoint(), payerUserId, discount.paidAmount(locked.totalPrice()), PaymentState.READY));
        }
        final String pgOrderId = "payment_" + UUID.randomUUID().toString().replace("-", "");
        final PaymentAttempt attempt = attempts.save(new PaymentAttempt(payment.id(), locked.key(), pgOrderId, requestHash, inputHash));
        reservePoints.execute(locked.userId(), attempt.id(), payment.usedPoint(), payment.id());
        return new PaymentCreationResult(payment.id(), pgOrderId);
    }

    private Order lockAndValidate(final String orderKey, final Long userId) {
        final Order order = lockOrder.execute(orderKey);
        if (!order.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        if (order.status() != OrderStatus.CREATED || order.expiresAt() == null || !order.expiresAt().isAfter(LocalDateTime.now())) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        return order;
    }

    private Optional<PaymentCreationResult> existingReady(final Long orderId, final String orderKey, final String requestHash,
        final String inputHash) {
        final Optional<PaymentAttempt> request = attempts.findByRequestHash(requestHash);
        if (request.isPresent()) {
            final PaymentAttempt attempt = request.get();
            if (!attempt.orderKey().equals(orderKey) || !attempt.inputHash().equals(inputHash)) {
                throw new CoreException(ErrorCode.PG_REQUEST_CONFLICT);
            }
            if (attempt.state() != PaymentAttemptState.READY) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            if (operations.existsPending(orderKey)) {
                throw new CoreException(ErrorCode.PG_PENDING);
            }
            return Optional.of(new PaymentCreationResult(attempt.paymentId(), attempt.pgOrderId()));
        }
        final Optional<Payment> payment = payments.findByOrderId(orderId);
        if (payment.isEmpty()) {
            return Optional.empty();
        }
        if (payment.get().state() == PaymentState.SUCCESS) {
            throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
        }
        final PaymentAttempt attempt =
            attempts.findLatestByPaymentId(payment.get().id()).orElseThrow(() -> new CoreException(ErrorCode.PG_REVIEW_REQUIRED));
        if (attempt.state() == PaymentAttemptState.READY) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
        if (attempt.state() == PaymentAttemptState.COMPLETED) {
            throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
        }
        return Optional.empty();
    }
}
