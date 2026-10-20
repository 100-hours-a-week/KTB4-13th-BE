package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
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
        final Payment payment = getPaymentUseCase.execute(order.key());
        final CancelBalance cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        if (!targetItem.order().id().equals(order.id())) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CANCELED);
        }
        if (!payment.orderId().equals(order.id())) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        final BigDecimal pointAmount = targetItem.pointAllocationForNextCanceledUnits(action.quantity());
        final BigDecimal couponAmount = targetItem.couponDiscountForRemainingQuantity()
            .subtract(targetItem.couponDiscountForQuantity(targetItem.cancellableQuantity() - action.quantity()));
        final BigDecimal paidAmount =
            targetItem.remainingGeneralPaymentAmount().subtract(targetItem.remainingGeneralPaymentAmountAfterCancel(action.quantity()));
        if (paidAmount.signum() < 0 || paidAmount.compareTo(cancelBalance.cancellablePaidAmount()) > 0
            || pointAmount.compareTo(cancelBalance.cancellablePointAmount()) > 0
            || couponAmount.compareTo(cancelBalance.cancellableCouponAmount()) > 0) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        final boolean restoreCoupon = targetItem.hasAppliedCoupon() && action.quantity() == targetItem.cancellableQuantity();
        return new CancelCalculated(paidAmount, couponAmount, pointAmount, restoreCoupon);
    }
}
