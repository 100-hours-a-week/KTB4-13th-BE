package com.book.core.notification.application.event;

public record NotificationStreamMessage(String eventName, Long notificationId) {
    public static NotificationStreamMessage notification(final Long notificationId) {
        return new NotificationStreamMessage("notification", notificationId);
    }

    public static NotificationStreamMessage syncRequired() {
        return new NotificationStreamMessage("notification-sync-required", null);
    }
}
