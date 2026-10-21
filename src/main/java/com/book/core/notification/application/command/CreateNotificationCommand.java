package com.book.core.notification.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record CreateNotificationCommand(Long userId, String title, String body) {
    public CreateNotificationCommand {
        if (userId == null || userId <= 0 || title == null || title.isBlank() || title.length() > 100 || body == null || body.isBlank()
            || body.length() > 1000) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
