package com.book.core.payment.api.converter;

import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** HTTP 입력과 참고 코드의 미입력 기본값을 Command로 옮깁니다. */
@Component
public class PaymentCommandConverter {
    /** HTTP 입력을 옮기고 미입력 쿠폰과 포인트에 참고 코드의 -1 기본값을 적용합니다. */
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

    public FailPaymentCommand toFailCommand(final String orderId, final String code, final String message) {
        return new FailPaymentCommand(orderId, code, message);
    }
}
