package com.book.core.payment.api.converter;

import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandConverter {

    public CreatePaymentCommand toCreateCommand(final Long userId, final CreatePaymentRequest request, final String requestKey) {
        Long useOwnedCouponId = request.useOwnedCouponId();
        if (useOwnedCouponId == null) {
            useOwnedCouponId = -1L;
        }
        BigDecimal usePoint = request.usePoint();
        if (usePoint == null) {
            usePoint = BigDecimal.valueOf(-1);
        }
        return new CreatePaymentCommand(userId, request.orderKey(), useOwnedCouponId, usePoint, requestKey);
    }

    public ApprovePaymentCommand toApproveCommand(final String orderKey, final String paymentKey, final BigDecimal amount) {
        return new ApprovePaymentCommand(orderKey, paymentKey, amount);
    }

    public FailPaymentCommand toFailCommand(final String orderKey, final String code, final String message) {
        return new FailPaymentCommand(orderKey, code, message);
    }
}
