package com.book.core.notification.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record GetNotificationsCommand(Long userId) {
    public GetNotificationsCommand {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
