package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelAmount;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.CancelCalculated;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.coupon.application.usecase.GetOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.application.usecase.GetOrderItemForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.domain.Payment;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CalculatePartialCancelUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetOrderItemForCancelUseCase getOrderItemForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final GetOwnedCouponUseCase getOwnedCouponUseCase;

    @Transactional(readOnly = true)
    public CancelCalculated execute(final PartialCancelAction action) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        final OrderItem targetItem = getOrderItemForCancelUseCase.execute(action.orderItemId());
        final Payment payment = getPaymentUseCase.execute(order.id());
        final CancelBalance cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        BigDecimal minOrderAmount = BigDecimal.ZERO;
        if (payment.hasAppliedCoupon()) {
            minOrderAmount = getOwnedCouponUseCase.execute(payment.ownedCouponId()).coupon().minOrderAmount();
        }
        final CancelAmount cancelAmount = new CancelAmount(targetItem.unitPrice().multiply(BigDecimal.valueOf(action.quantity())),
            order.totalPrice(), cancelBalance.totalCanceledAmount(), cancelBalance.cancellablePaidAmount(),
            cancelBalance.cancellablePointAmount(), cancelBalance.cancellableCouponAmount(), minOrderAmount);
        return new CancelCalculated(cancelAmount.paidAmount(), cancelAmount.couponAmount(), cancelAmount.pointAmount(),
            cancelAmount.isRestoreCoupon());
    }
}
