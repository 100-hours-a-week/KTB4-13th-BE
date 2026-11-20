package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class NotificationRepositoryAdapter implements NotificationRepositoryPort {
    private final NotificationJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Notification save(final Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public boolean insertIfEventKeyAbsent(final Notification notification) {
        try {
            jdbcTemplate.update("INSERT INTO notifications (user_id, title, body, event_key) VALUES (?, ?, ?, ?)", notification.userId(),
                notification.title(), notification.body(), notification.eventKey());
            return true;
        } catch (final DuplicateKeyException exception) {
            final Throwable cause = exception.getMostSpecificCause();
            if (cause instanceof SQLException sqlException && sqlException.getErrorCode() == 1062
                && String.valueOf(sqlException.getMessage()).contains("uk_notifications_event_key")) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public List<Notification> findRecentByUserId(final Long userId) {
        return jpaRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByIdDesc(userId);
    }

    @Override
    public long countUnreadByUserId(final Long userId) {
        return jpaRepository.countByUserIdAndDeletedAtIsNullAndReadAtIsNull(userId);
    }

    @Override
    public Optional<Notification> findOwnedActiveForUpdate(final Long userId, final Long notificationId) {
        return jpaRepository.findOwnedActiveForUpdate(userId, notificationId);
    }
}
