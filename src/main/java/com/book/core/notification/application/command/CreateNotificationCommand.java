package com.book.core.notification.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record CreateNotificationCommand(Long userId, String title, String body, String eventKey) {
    public CreateNotificationCommand {
        if (userId == null || userId <= 0 || title == null || title.isBlank() || title.length() > 100 || body == null || body.isBlank()
            || body.length() > 1000 || (eventKey != null && (eventKey.isBlank() || eventKey.length() > 150))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    public CreateNotificationCommand(final Long userId, final String title, final String body) {
        this(userId, title, body, null);
    }
}
