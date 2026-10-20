package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.RecordPaymentHistoryUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.RestorePaymentPointsUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelPaymentUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final RevertOwnedCouponUseCase revertOwnedCouponUseCase;
    private final RestorePaymentPointsUseCase restorePaymentPointsUseCase;
    private final CancelRepositoryPort cancelRepository;
    private final RecordPaymentHistoryUseCase recordPaymentHistoryUseCase;

    @Transactional
    public Long execute(final CancelAction action) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        if ((order.status() != OrderStatus.PAID && order.status() != OrderStatus.PARTIAL_CANCELED)
            || order.items().stream().anyMatch((final var item) -> item.status() == OrderItemStatus.CONFIRMED)) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CANCELED);
        }
        final Payment payment = getPaymentUseCase.execute(order.key());
        final CancelBalance cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        order.canceled();
        order.items().stream().filter((final var item) -> item.hasAppliedCoupon()).map((final var item) -> item.ownedCouponId()).distinct()
            .forEach(revertOwnedCouponUseCase::execute);
        final BigDecimal refundedPaidAmount = cancelBalance.cancellablePaidAmount();
        final BigDecimal canceledPointAmount = cancelBalance.cancellablePointAmount();
        final BigDecimal canceledCouponAmount = cancelBalance.cancellableCouponAmount();
        cancelBalance.cancel(refundedPaidAmount, canceledPointAmount, canceledCouponAmount);
        final Long representativeOwnedCouponId = order.items().stream().filter((final var item) -> item.hasAppliedCoupon())
            .map((final var item) -> item.ownedCouponId()).findFirst().orElse(-1L);
        final Cancel cancel = cancelRepository.save(new Cancel(payment.userId(), CancelType.ALL, payment.orderId(), -1L, payment.id(),
            payment.originAmount(), representativeOwnedCouponId, canceledCouponAmount, canceledPointAmount, refundedPaidAmount, -1L,
            refundedPaidAmount, canceledPointAmount, canceledCouponAmount, "PG_API_응답_취소_고유_값_저장", LocalDateTime.now()));
        final BigDecimal restoredPointAmount =
            restorePaymentPointsUseCase.execute(payment.userId(), payment.id(), cancel.id(), canceledPointAmount);
        cancel.recordPointRestoration(restoredPointAmount);
        cancelRepository.save(cancel);
        recordPaymentHistoryUseCase.execute(new PaymentHistory(TransactionType.CANCEL, payment.userId(), payment.orderId(), payment.id(),
            payment.externalPaymentKey(), refundedPaidAmount, restoredPointAmount, canceledCouponAmount, "취소 성공", cancel.canceledAt()));
        log.info("결제 전체 취소 처리를 마쳤습니다. cancelId={}, paymentId={}, orderId={}, traceId={}", cancel.id(), payment.id(), order.id(),
            MDC.get("traceId"));
        return cancel.id();
    }
}
