package com.book.core.order.application.result;

import com.book.core.cancel.domain.CancelBalance;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record OrderPaymentResult(
        BigDecimal couponDiscount,
        BigDecimal usedPoint,
        BigDecimal paidAmount,
        PaymentMethod method,
        LocalDateTime paidAt,
        BigDecimal cancelledPaidAmount,
        BigDecimal cancellablePaidAmount) {
    public static OrderPaymentResult of(final Payment payment, final CancelBalance cancelBalance) {
        return new OrderPaymentResult(payment.couponDiscount(), payment.usedPoint(), payment.paidAmount(), payment.method(),
                payment.paidAt(), cancelBalance.cancelledPaidAmount(), cancelBalance.cancellablePaidAmount());
    }
}
// @formatter:on
