package com.book.core.notification.application.port;

import com.book.core.notification.domain.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(final Notification notification);

    Notification saveIfAbsent(final Notification notification);

    List<Notification> findRecentByUserId(final Long userId);

    long countUnreadByUserId(final Long userId);

    Optional<Notification> findOwnedActiveForUpdate(final Long userId, final Long notificationId);
}
