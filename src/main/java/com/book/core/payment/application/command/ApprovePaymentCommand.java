package com.book.core.payment.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;
import java.math.BigDecimal;

public record ApprovePaymentCommand(String orderKey, String pgOrderId, String externalPaymentKey, BigDecimal amount, Long attemptId) {
    public ApprovePaymentCommand(final String orderKey, final String externalPaymentKey, final BigDecimal amount) {
        this(orderKey, orderKey, externalPaymentKey, amount, null);
    }

    public ApprovePaymentCommand {
        if (orderKey == null || orderKey.isBlank() || pgOrderId == null || pgOrderId.isBlank() || amount == null || amount.signum() < 0
            || amount.stripTrailingZeros().scale() > 0
            || (amount.signum() > 0 && (externalPaymentKey == null || externalPaymentKey.isBlank()))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public PreparePgOperationCommand operation() {
        final String input = pgOrderId + ":" + externalPaymentKey + ":" + amount.stripTrailingZeros().toPlainString();
        return new PreparePgOperationCommand(orderKey, null, PgOperationType.APPROVE,
            "approve_" + PgOperation.hash(pgOrderId + ":" + externalPaymentKey), input, amount, null, 0, externalPaymentKey, pgOrderId,
            attemptId);
    }

    @Override
    public String toString() {
        return "ApprovePaymentCommand[redacted]";
    }
}
