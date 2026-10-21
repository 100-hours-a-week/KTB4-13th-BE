package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop20ByUserIdAndDeletedAtIsNullOrderByIdDesc(final Long userId);

    long countByUserIdAndDeletedAtIsNullAndReadAtIsNull(final Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :notificationId and n.userId = :userId and n.deletedAt is null")
    Optional<Notification> findOwnedActiveForUpdate(@Param("userId") final Long userId, @Param("notificationId") final Long notificationId);
}
