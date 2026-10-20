package com.book.core.notification.application.result;

import java.time.LocalDateTime;

public record NotificationItemResult(Long notificationId, String title, String body, LocalDateTime createdAt, LocalDateTime readAt) {
}
