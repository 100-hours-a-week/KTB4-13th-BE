package com.book.core.notification.application.port;

import com.book.core.notification.domain.Notification;
import java.time.LocalDateTime;

public interface NotificationRepositoryPort {
    Notification save(final Notification notification);

    Notification saveIfAbsent(final Notification notification);

    void deleteAllByUserId(final Long userId);

    void deleteCreatedBefore(final LocalDateTime createdAt);
}
