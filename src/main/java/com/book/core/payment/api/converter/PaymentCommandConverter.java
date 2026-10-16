package com.book.core.payment.api.converter;

import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandConverter {

    public CreatePaymentCommand toCreateCommand(final Long userId, final CreatePaymentRequest request) {
        Long useOwnedCouponId = request.useOwnedCouponId();
        if (useOwnedCouponId == null) {
            useOwnedCouponId = -1L;
        }
        BigDecimal usePoint = request.usePoint();
        if (usePoint == null) {
            usePoint = BigDecimal.valueOf(-1);
        }
        return new CreatePaymentCommand(userId, request.orderKey(), useOwnedCouponId, usePoint);
    }

    /** 외부 orderId를 주문 키로 해석해 승인 입력을 Command로 옮깁니다. */
    public ApprovePaymentCommand toApproveCommand(final String orderId, final String paymentKey, final BigDecimal amount) {
        return new ApprovePaymentCommand(orderId, paymentKey, amount);
    }

}
