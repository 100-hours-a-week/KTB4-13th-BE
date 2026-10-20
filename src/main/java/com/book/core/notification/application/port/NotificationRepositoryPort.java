package com.book.core.notification.application.port;

import com.book.core.notification.domain.Notification;

public interface NotificationRepositoryPort {
    Notification save(final Notification notification);

    Notification saveIfAbsent(final Notification notification);
}
