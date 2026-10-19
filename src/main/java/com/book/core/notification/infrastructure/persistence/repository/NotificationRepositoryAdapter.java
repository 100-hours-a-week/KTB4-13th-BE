package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
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
}
