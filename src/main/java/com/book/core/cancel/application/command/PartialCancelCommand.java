package com.book.core.cancel.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.domain.PartialCancelAction;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.domain.PgOperationType;

public record PartialCancelCommand(Long userId, PartialCancelAction action, String requestKey) {
    public PartialCancelCommand {
        if (userId == null || userId <= 0 || action == null || action.orderItemId() == null || action.orderItemId() <= 0
            || action.quantity() <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public PreparePgOperationCommand operation() {
        return new PreparePgOperationCommand(action.orderKey(), userId, PgOperationType.PARTIAL_CANCEL, requestKey,
            "PARTIAL_CANCEL:" + action.orderKey() + ":" + action.orderItemId() + ":" + action.quantity(), null, action.orderItemId(),
            action.quantity());
    }
}
