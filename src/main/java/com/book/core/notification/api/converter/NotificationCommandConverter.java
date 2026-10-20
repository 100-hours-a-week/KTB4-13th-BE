package com.book.core.notification.api.converter;

import com.book.core.notification.application.command.GetNotificationsCommand;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandConverter {
    public GetNotificationsCommand toGetCommand(final Long userId) {
        return new GetNotificationsCommand(userId);
    }
}
