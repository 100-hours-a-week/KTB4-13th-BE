package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.usecase.CreateCancelBalanceUseCase;
import com.book.core.coupon.application.usecase.UseOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentHistoryRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.application.result.ApprovedPaymentResult;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.domain.enums.PointType;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CompletePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final UseOwnedCouponUseCase useOwnedCouponUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final CreateCancelBalanceUseCase createCancelBalanceUseCase;
    private final PaymentHistoryRepositoryPort paymentHistoryRepository;

    @Transactional
    public Long execute(final String orderKey, final ApprovedPaymentResult approval) {
        final Order order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.CREATED);
        final Payment payment = paymentRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        payment.success(approval.paymentKey(), approval.method(), approval.approveCode());
        order.paid();
        order.items().stream().filter((final var item) -> item.hasAppliedCoupon()).map((final var item) -> item.ownedCouponId()).distinct()
            .forEach(useOwnedCouponUseCase::execute);
        // TODO(#330): 승인 시 사용 포인트 부족을 재검증하거나 예약하고, 보너스 회수 정책과 분리한다.
        final var pointAllocationsByOrderItem = new LinkedHashMap<Long, java.math.BigDecimal>();
        order.items().stream().filter((final var item) -> item.usedPointAmount().signum() > 0)
            .forEach((final var item) -> pointAllocationsByOrderItem.put(item.id(), item.usedPointAmount()));
        if (pointAllocationsByOrderItem.isEmpty()) {
            deductPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), payment.usedPoint());
        } else {
            deductPointUseCase.execute(payment.userId(), PointType.PAYMENT, payment.id(), payment.usedPoint(), pointAllocationsByOrderItem);
        }
        createCancelBalanceUseCase.execute(order.id(), payment.paidAmount(), payment.usedPoint(), payment.couponDiscount());
        paymentHistoryRepository.save(new PaymentHistory(TransactionType.PAYMENT, payment.userId(), payment.orderId(), payment.id(),
            approval.paymentKey(), payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), "결제 성공", payment.paidAt()));
        log.info("결제 승인 처리를 마쳤습니다. paymentId={}, orderId={}, traceId={}", payment.id(), order.id(), MDC.get("traceId"));
        return payment.id();
    }
}
