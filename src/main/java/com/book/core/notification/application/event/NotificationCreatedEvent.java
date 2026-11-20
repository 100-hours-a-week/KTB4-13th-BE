package com.book.core.notification.application.event;

public record NotificationCreatedEvent(Long userId, Long notificationId) {
}
