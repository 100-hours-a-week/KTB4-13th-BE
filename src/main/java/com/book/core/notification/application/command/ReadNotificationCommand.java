package com.book.core.notification.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record ReadNotificationCommand(Long userId, Long notificationId) {
    public ReadNotificationCommand {
        if (userId == null || userId <= 0 || notificationId == null || notificationId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
