package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByUserIdAndDeletedAtIsNullOrderByIdDesc(final Long userId);

    long countByUserIdAndDeletedAtIsNullAndReadAtIsNull(final Long userId);
}
