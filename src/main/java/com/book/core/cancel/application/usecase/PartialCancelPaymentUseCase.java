package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
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
public class PartialCancelPaymentUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final RevertOwnedCouponUseCase revertOwnedCouponUseCase;
    private final RestorePaymentPointsUseCase restorePaymentPointsUseCase;
    private final CancelRepositoryPort cancelRepository;
    private final RecordPaymentHistoryUseCase recordPaymentHistoryUseCase;
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;

    @Transactional
    public Long execute(final PartialCancelAction action, final CancelCalculated calculated) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        final Payment payment = getPaymentUseCase.execute(order.key());
        final CancelBalance cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final OrderItem targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
        targetItem.cancel(action.quantity());
        if (order.items().stream().allMatch(OrderItem::isAllCanceled)) {
            order.canceled();
        } else {
            order.partialCanceled();
        }
        if (calculated.isRestoreCoupon()) {
            revertOwnedCouponUseCase.execute(targetItem.ownedCouponId());
        }
        cancelBalance.cancel(calculated.paidAmount(), calculated.pointAmount(), calculated.couponAmount());
        final Cancel cancel = cancelRepository.save(
            new Cancel(payment.userId(), CancelType.PARTIAL, payment.orderId(), action.orderItemId(), payment.id(), payment.originAmount(),
                targetItem.ownedCouponId(), calculated.couponAmount(), calculated.pointAmount(), calculated.paidAmount(), action.quantity(),
                calculated.paidAmount(), calculated.pointAmount(), calculated.couponAmount(), "PG_API_응답_취소_고유_값_저장", LocalDateTime.now()));
        final BigDecimal restoredPointAmount = restorePaymentPointsUseCase.execute(payment.userId(), payment.id(), cancel.id(),
            calculated.pointAmount(), action.orderItemId());
        cancel.recordPointRestoration(restoredPointAmount);
        cancelRepository.save(cancel);
        recordPaymentHistoryUseCase.execute(new PaymentHistory(TransactionType.PARTIAL_CANCEL, payment.userId(), payment.orderId(),
            payment.id(), payment.externalPaymentKey(), calculated.paidAmount(), restoredPointAmount, calculated.couponAmount(), "부분 취소 성공",
            cancel.canceledAt()));
        log.info("결제 부분 취소 처리를 마쳤습니다. cancelId={}, paymentId={}, orderId={}, traceId={}", cancel.id(), payment.id(), order.id(),
            MDC.get("traceId"));
        return cancel.id();
    }
}
