package com.book.core.payment.api.converter;

import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
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

    public ApprovePaymentCommand toApproveCommand(final String orderId, final String paymentKey, final BigDecimal amount) {
        return new ApprovePaymentCommand(orderId, paymentKey, amount);
    }

    /** 주문 키와 실패 코드 및 메시지를 업무 이력 기록용 Command로 옮깁니다. */
    public FailPaymentCommand toFailCommand(final String orderId, final String code, final String message) {
        return new FailPaymentCommand(orderId, code, message);
    }
}
