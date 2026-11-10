package com.book.core.payment.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.application.usecase.GetOrderUseCase;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import com.book.core.payment.application.result.PaymentCompletionResult;
import com.book.core.payment.application.result.PaymentCreationResult;
import com.book.core.payment.application.usecase.ApprovePgPaymentUseCase;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.application.usecase.FailPaymentAttemptUseCase;
import com.book.core.payment.application.usecase.RecordPaymentFailureCallbackUseCase;
import com.book.core.payment.application.usecase.MarkPgOperationUncertainUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.ResolvePaymentAttemptUseCase;
import com.book.core.payment.domain.PaymentAttempt;
import com.book.core.payment.domain.PaymentCoupon;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.PgOperation;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final GetCouponTargetsUseCase getCouponTargetsUseCase;
    private final FindOwnedCouponsForCheckoutUseCase findOwnedCouponsForCheckoutUseCase;
    private final GetPointBalanceUseCase getPointBalanceUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final ApprovePgPaymentUseCase approvePgPaymentUseCase;
    private final PreparePgOperationUseCase preparePgOperationUseCase;
    private final FailPaymentAttemptUseCase failPaymentAttemptUseCase;
    private final MarkPgOperationUncertainUseCase markUncertain;
    private final ResolvePaymentAttemptUseCase resolveAttempt;
    private final RecordPaymentFailureCallbackUseCase recordFailureCallback;
    private final PostProcessPaymentUseCase postProcessPaymentUseCase;

    public PaymentCreationResult create(final CreatePaymentCommand command) {
        final String inputHash = PgOperation.hash(command.userId() + ":" + command.orderKey() + ":" + command.useOwnedCouponId() + ":"
            + command.usePoint().stripTrailingZeros().toPlainString());
        final String requestHash = PgOperation.hash(command.userId() + ":" + command.requestKey());
        final Optional<PaymentCreationResult> existing =
            createPaymentUseCase.findExisting(command.orderKey(), command.userId(), requestHash, inputHash);
        if (existing.isPresent()) {
            return existing.get();
        }
        final Order order = getPaymentOrderUseCase.execute(command.userId(), command.orderKey());
        final Set<Long> couponIds = getCouponTargetsUseCase.execute(order.items().stream().map(OrderItem::productId).toList());
        final List<PaymentCoupon> ownedCoupons =
            findOwnedCouponsForCheckoutUseCase.execute(command.userId(), couponIds, LocalDateTime.now()).stream()
                .map((final OwnedCouponResult item) -> new PaymentCoupon(item.id(), item.coupon().type(), item.coupon().discount(),
                    item.coupon().minOrderAmount()))
                .toList();
        final BigDecimal pointBalance = getPointBalanceUseCase.execute(command.userId());
        final PaymentDiscount paymentDiscount =
            new PaymentDiscount(ownedCoupons, pointBalance, command.useOwnedCouponId(), command.usePoint());
        return createPaymentUseCase.execute(order, command.userId(), paymentDiscount, requestHash, inputHash);
    }

    public Long success(final ApprovePaymentCommand incoming) {
        final ApprovePaymentCommand command = normalize(incoming);
        try {
            preparePgOperationUseCase.execute(command.operation());
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PG_REVIEW_REQUIRED) {
                markUncertainSafely(command, exception, true);
            }
            throw exception;
        }
        final PaymentCompletionResult result;
        try {
            result = approvePgPaymentUseCase.execute(command);
        } catch (final RuntimeException exception) {
            try {
                if (exception instanceof CoreException core && core.errorCode() == ErrorCode.PG_REJECTED) {
                    failPaymentAttemptUseCase.execute(command.operation());
                } else {
                    final boolean urgent = exception instanceof CoreException core
                        && (core.errorCode() == ErrorCode.PG_RESPONSE_MISMATCH || core.errorCode() == ErrorCode.PG_REVIEW_REQUIRED);
                    markUncertainSafely(command, exception, urgent);
                }
            } catch (final RuntimeException recordingFailure) {
                log.error("[PAYMENT] 결제 결과 기록에 실패했습니다. 운영 확인이 필요합니다.");
                exception.addSuppressed(recordingFailure);
            }
            throw exception;
        }
        if (result.completedNow()) {
            postProcessPaymentUseCase.execute(result.paymentId(), command.orderKey());
        }
        return result.paymentId();
    }

    public Long successForUser(final Long userId, final ApprovePaymentCommand incoming) {
        final PaymentAttempt attempt = resolveAttempt.execute(incoming.pgOrderId());
        getOrderUseCase.execute(new GetOrderCommand(userId, attempt.orderKey()));
        return success(new ApprovePaymentCommand(attempt.orderKey(), attempt.pgOrderId(), incoming.externalPaymentKey(), incoming.amount(),
            attempt.id()));
    }

    public void fail(final Long userId, final FailPaymentCommand command) {
        final PaymentAttempt attempt = resolveAttempt.execute(command.orderKey());
        if (recordFailureCallback.execute(userId, attempt.orderKey(), attempt.id(), command.code(), command.message())) {
            throw new CoreException(ErrorCode.PG_PENDING);
        }
    }

    private ApprovePaymentCommand normalize(final ApprovePaymentCommand incoming) {
        if (incoming.attemptId() != null) {
            return incoming;
        }
        final PaymentAttempt attempt = resolveAttempt.execute(incoming.pgOrderId());
        return new ApprovePaymentCommand(attempt.orderKey(), attempt.pgOrderId(), incoming.externalPaymentKey(), incoming.amount(),
            attempt.id());
    }

    private void markUncertainSafely(final ApprovePaymentCommand command, final RuntimeException original, final boolean urgent) {
        try {
            markUncertain.execute(command.operation(), urgent);
        } catch (final RuntimeException recordingFailure) {
            log.error("[PAYMENT] 결제 결과 기록에 실패했습니다. 운영 확인이 필요합니다.");
            original.addSuppressed(recordingFailure);
        }
    }
}
