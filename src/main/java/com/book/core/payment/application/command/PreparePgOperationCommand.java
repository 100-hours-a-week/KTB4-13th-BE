package com.book.core.payment.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;

public record PreparePgOperationCommand(String orderKey, Long userId, PgOperationType type, String requestKey, String input,
    java.math.BigDecimal amount, Long orderItemId, long quantity, String externalPaymentKey, String pgOrderId, Long attemptId) {
    public PreparePgOperationCommand(final String orderKey, final Long userId, final PgOperationType type, final String requestKey,
        final String input, final java.math.BigDecimal amount, final Long orderItemId, final long quantity) {
        this(orderKey, userId, type, requestKey, input, amount, orderItemId, quantity, null, null, null);
    }

    public PreparePgOperationCommand {
        if (orderKey == null || orderKey.isBlank() || type == null || requestKey == null || !requestKey.matches("[A-Za-z0-9_-]{1,100}")
            || input == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String requestHash() {
        return PgOperation.hash(String.valueOf(userId) + ":" + requestKey);
    }

    public String inputHash() {
        return PgOperation.hash(input);
    }

    /** Identifies an inconsistent approval callback without consuming the normal request key. */
    public String isolatedApprovalHash() {
        return PgOperation.hash("isolated-approval:" + orderKey + ":" + requestHash() + ":" + inputHash());
    }

    @Override
    public String toString() {
        return "PreparePgOperationCommand[redacted]";
    }
}
