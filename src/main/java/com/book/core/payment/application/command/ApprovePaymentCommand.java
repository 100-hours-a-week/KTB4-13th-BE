package com.book.core.payment.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;

public record ApprovePaymentCommand(String orderKey, String externalPaymentKey, BigDecimal amount) {
    public ApprovePaymentCommand {
        if (orderKey == null || orderKey.isBlank() || amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0
            || (amount.signum() > 0 && (externalPaymentKey == null || externalPaymentKey.isBlank()))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public PreparePgOperationCommand operation() {
        final String input = orderKey + ":" + externalPaymentKey + ":" + amount.stripTrailingZeros().toPlainString();
        return new PreparePgOperationCommand(orderKey, null, PgOperationType.APPROVE,
            "approve_" + PgOperation.hash(orderKey + ":" + externalPaymentKey), input, amount, null, 0);
    }

    @Override
    public String toString() {
        return "ApprovePaymentCommand[redacted]";
    }
}
