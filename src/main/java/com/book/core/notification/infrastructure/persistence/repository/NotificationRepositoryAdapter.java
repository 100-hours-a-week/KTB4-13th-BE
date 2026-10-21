package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class NotificationRepositoryAdapter implements NotificationRepositoryPort {
    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(final Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public Notification saveIfAbsent(final Notification notification) {
        jpaRepository.insertIfAbsent(notification.userId(), notification.title(), notification.body(), notification.eventKey());
        return jpaRepository.findByEventKeyAndDeletedAtIsNull(notification.eventKey()).orElseThrow();
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
