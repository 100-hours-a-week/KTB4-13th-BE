package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelAmount;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.coupon.application.usecase.GetOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 참고 계산처럼 취소 금액을 쿠폰, 결제 금액, 포인트로 배분합니다. */
@UseCase
@RequiredArgsConstructor
public class CalculatePartialCancelUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final GetOwnedCouponUseCase getOwnedCouponUseCase;

    /** 주문상품 금액과 남은 잔액으로 취소액을 쿠폰, 실결제액, 포인트 순서로 배분합니다. */
    @Transactional(readOnly = true)
    public CancelCalculated execute(final PartialCancelAction action) {
        final var order = getOrderForCancelUseCase.execute(action.orderKey());
        final var targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
        final var payment = getPaymentUseCase.execute(order.id());
        final var cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        BigDecimal minOrderAmount = BigDecimal.ZERO;
        if (payment.hasAppliedCoupon()) {
            minOrderAmount = getOwnedCouponUseCase.execute(payment.ownedCouponId()).coupon().minOrderAmount();
        }
        final var cancelAmount = new CancelAmount(targetItem.unitPrice().multiply(BigDecimal.valueOf(action.quantity())),
            order.totalPrice(), cancelBalance.totalCanceledAmount(), cancelBalance.cancellablePaidAmount(),
            cancelBalance.cancellablePointAmount(), cancelBalance.cancellableCouponAmount(), minOrderAmount);
        return new CancelCalculated(cancelAmount.paidAmount(), cancelAmount.couponAmount(), cancelAmount.pointAmount(),
            cancelAmount.isRestoreCoupon());
    }
}
