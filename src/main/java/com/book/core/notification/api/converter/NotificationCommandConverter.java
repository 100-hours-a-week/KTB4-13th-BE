package com.book.core.notification.api.converter;

import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.command.ReadNotificationCommand;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandConverter {
    public GetNotificationsCommand toGetCommand(final Long userId) {
        return new GetNotificationsCommand(userId);
    }

    public ReadNotificationCommand toReadCommand(final Long userId, final Long notificationId) {
        return new ReadNotificationCommand(userId, notificationId);
    }
}
