package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
}
