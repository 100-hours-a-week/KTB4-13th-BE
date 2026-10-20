package com.book.core.cancel.application.command;

import com.book.core.cancel.domain.CancelAction;
import com.book.core.payment.application.command.PreparePgOperationCommand;
import com.book.core.payment.domain.PgOperationType;

public record CancelCommand(Long userId, CancelAction action, String requestKey) {
    public PreparePgOperationCommand operation() {
        return new PreparePgOperationCommand(action.orderKey(), userId, PgOperationType.CANCEL, requestKey, "CANCEL:" + action.orderKey(),
            null, null, 0);
    }
}
