package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.RecordPaymentHistoryUseCase;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

/** 참고 부분 취소의 수량 전이, 혜택 복원, 잔액과 이력 기록입니다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class PartialCancelPaymentUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final RevertOwnedCouponUseCase revertOwnedCouponUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final CancelRepositoryPort cancelRepository;
    private final RecordPaymentHistoryUseCase recordPaymentHistoryUseCase;
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;

    /** 상품의 누적 취소 수량을 변경하고 계산된 환급액을 반영합니다. 지급 포인트는 첫 취소에서만 회수하며 모든 변경은 원자적으로 저장합니다. */
    @Transactional
    public Long execute(final PartialCancelAction action, final CancelCalculated calculated) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        final var payment = getPaymentUseCase.execute(order.id());
        final var cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        final var targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
        targetItem.cancel(action.quantity());
        if (order.items().stream().allMatch(OrderItem::isAllCanceled)) {
            order.canceled();
        } else {
            order.partialCanceled();
        }
        if (calculated.isRestoreCoupon()) {
            revertOwnedCouponUseCase.execute(payment.ownedCouponId());
        }
        if (calculated.pointAmount().compareTo(BigDecimal.ZERO) > 0) {
            earnPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), calculated.pointAmount());
        }
        // NOTE: 한 번이라도 취소가 생기면 결제 시 지급한 포인트 회수
        if (cancelRepository.countByOrderId(order.id()) == 0L) {
            deductPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), PointAmount.PAYMENT);
        }
        cancelBalance.cancel(calculated.paidAmount(), calculated.pointAmount(), calculated.couponAmount());
        final var cancel = cancelRepository.save(
            new Cancel(payment.userId(), CancelType.PARTIAL, payment.orderId(), action.orderItemId(), payment.id(), payment.originAmount(),
                payment.ownedCouponId(), calculated.couponAmount(), calculated.pointAmount(), calculated.paidAmount(), action.quantity(),
                calculated.paidAmount(), calculated.pointAmount(), calculated.couponAmount(), "PG_API_응답_취소_고유_값_저장", LocalDateTime.now()));
        recordPaymentHistoryUseCase.execute(new PaymentHistory(TransactionType.PARTIAL_CANCEL, payment.userId(), payment.orderId(),
            payment.id(), payment.externalPaymentKey(), calculated.paidAmount(), calculated.pointAmount(), calculated.couponAmount(),
            "부분 취소 성공", cancel.canceledAt()));
        log.info("결제 부분 취소 처리를 마쳤습니다. cancelId={}, paymentId={}, orderId={}, traceId={}", cancel.id(), payment.id(), order.id(),
            MDC.get("traceId"));
        return cancel.id();
    }
}
