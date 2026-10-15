package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CreateCancelBalanceUseCase;
import com.book.core.coupon.application.usecase.UseOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.PaymentMethod;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.PointAmount;
import com.book.core.point.domain.enums.PointType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

/** 참고 PaymentProcessor.success의 쓰기 작업을 한 트랜잭션에서 수행합니다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CompletePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final UseOwnedCouponUseCase useOwnedCouponUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final CreateCancelBalanceUseCase createCancelBalanceUseCase;
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    /** 결제를 SUCCESS, 주문과 상품을 PAID로 전환하고 쿠폰 사용, 포인트 변경, 취소 잔액 초기화와 결제 이력을 같은 트랜잭션에 저장합니다. */
    @Transactional
    public Long execute(final String orderKey, final String externalPaymentKey) {
        final var order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.CREATED);
        final var payment = paymentRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        payment.success(externalPaymentKey, PaymentMethod.CARD, "PG 승인 API 호출의 응답 값 중 `승인번호` 넣기");
        order.paid();
        if (payment.hasAppliedCoupon()) {
            useOwnedCouponUseCase.execute(payment.ownedCouponId());
        }
        deductPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), payment.usedPoint());
        earnPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), PointAmount.PAYMENT);
        createCancelBalanceUseCase.execute(order.id(), payment.paidAmount(), payment.usedPoint(), payment.couponDiscount());
        paymentHistoryRepository.save(new PaymentHistory(TransactionType.PAYMENT, payment.userId(), payment.orderId(), payment.id(),
            externalPaymentKey, payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), "결제 성공", payment.paidAt()));
        log.info("결제 승인 처리를 마쳤습니다. paymentId={}, orderId={}, traceId={}", payment.id(), order.id(), MDC.get("traceId"));
        return payment.id();
    }
}
