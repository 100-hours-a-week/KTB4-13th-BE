package com.book.core.payment.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.domain.PgOperation;
import com.book.core.payment.domain.PgOperationType;

public record PreparePgOperationCommand(String orderKey, Long userId, PgOperationType type, String requestKey, String input,
    java.math.BigDecimal amount, Long orderItemId, long quantity) {
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

    @Override
    public String toString() {
        return "PreparePgOperationCommand[redacted]";
    }
}
