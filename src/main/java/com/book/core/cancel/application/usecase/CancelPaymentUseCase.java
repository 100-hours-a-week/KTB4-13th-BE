package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.RecordPaymentHistoryUseCase;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.enums.PointType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

/** 참고 전체 취소의 혜택 복원, 잔액 차감 및 취소 내역 기록입니다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelPaymentUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final RevertOwnedCouponUseCase revertOwnedCouponUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final CancelRepositoryPort cancelRepository;
    private final RecordPaymentHistoryUseCase recordPaymentHistoryUseCase;

    /** 주문 상태만 CANCELED로 바꾸고 쿠폰과 사용 포인트를 복원합니다. 지급 포인트 회수, 취소 잔액과 이력도 같은 트랜잭션에 기록합니다. */
    @Transactional
    public Long execute(final CancelAction action) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        if (order.status() != OrderStatus.PAID) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        final var payment = getPaymentUseCase.execute(order.id());
        final var cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        order.canceled();
        if (payment.hasAppliedCoupon()) {
            revertOwnedCouponUseCase.execute(payment.ownedCouponId());
        }
        earnPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), payment.usedPoint());
        deductPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), PointAmount.PAYMENT);
        cancelBalance.cancel(payment.paidAmount(), payment.usedPoint(), payment.couponDiscount());
        final var cancel = cancelRepository.save(new Cancel(payment.userId(), CancelType.ALL, payment.orderId(), -1L, payment.id(),
            payment.originAmount(), payment.ownedCouponId(), payment.couponDiscount(), payment.usedPoint(), payment.paidAmount(), -1L,
            payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), "PG_API_응답_취소_고유_값_저장", LocalDateTime.now()));
        recordPaymentHistoryUseCase.execute(
            new PaymentHistory(TransactionType.CANCEL, payment.userId(), payment.orderId(), payment.id(), payment.externalPaymentKey(),
                payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), "취소 성공", cancel.canceledAt()));
        log.info("결제 전체 취소 처리를 마쳤습니다. cancelId={}, paymentId={}, orderId={}, traceId={}", cancel.id(), payment.id(), order.id(),
            MDC.get("traceId"));
        return cancel.id();
    }
}
