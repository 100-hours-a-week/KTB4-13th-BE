package com.book.core.notification.api.response;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
}
