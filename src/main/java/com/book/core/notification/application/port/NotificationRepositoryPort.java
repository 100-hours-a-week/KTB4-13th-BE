package com.book.core.notification.application.port;

import com.book.core.notification.domain.Notification;
import java.util.List;

public interface NotificationRepositoryPort {
    Notification save(final Notification notification);

    List<Notification> findRecentByUserId(final Long userId);

    long countUnreadByUserId(final Long userId);
}
