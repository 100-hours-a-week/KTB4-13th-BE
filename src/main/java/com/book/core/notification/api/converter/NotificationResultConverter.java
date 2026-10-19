package com.book.core.notification.api.converter;

import com.book.core.notification.api.response.NotificationListResponse;
import com.book.core.notification.api.response.NotificationResponse;
import com.book.core.notification.application.result.GetNotificationsResult;
import org.springframework.stereotype.Component;

@Component
public class NotificationResultConverter {
    public NotificationListResponse toResponse(final GetNotificationsResult result) {
        final var notifications = result.notifications().stream().map(
            (final var item) -> new NotificationResponse(item.notificationId(), item.title(), item.body(), item.createdAt(), item.readAt()))
            .toList();
        return new NotificationListResponse(notifications, result.unreadCount());
    }
}
