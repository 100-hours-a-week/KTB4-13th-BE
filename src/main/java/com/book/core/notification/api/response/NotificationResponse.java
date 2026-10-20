package com.book.core.notification.api.response;

import java.time.LocalDateTime;

public record NotificationResponse(Long notificationId, String title, String body, LocalDateTime createdAt, LocalDateTime readAt) {
}
