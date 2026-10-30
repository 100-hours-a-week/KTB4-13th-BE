package com.book.core.payment.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.math.BigDecimal;

public record CreatePaymentCommand(Long userId, String orderKey, Long useOwnedCouponId, BigDecimal usePoint, String requestKey) {
    public CreatePaymentCommand {
        if (userId == null || orderKey == null || orderKey.isBlank() || useOwnedCouponId == null || usePoint == null || requestKey == null
            || !requestKey.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
