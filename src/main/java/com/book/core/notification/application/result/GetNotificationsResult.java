package com.book.core.notification.application.result;

import java.util.List;

public record GetNotificationsResult(List<NotificationItemResult> notifications, long unreadCount) {
    public GetNotificationsResult {
        notifications = List.copyOf(notifications);
    }
}
