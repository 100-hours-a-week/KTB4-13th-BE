package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.domain.Notification;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventKeyAndDeletedAtIsNull(final String eventKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        INSERT INTO notifications (user_id, title, body, event_key, created_at, updated_at)
        VALUES (:userId, :title, :body, :eventKey, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
        """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") final Long userId, @Param("title") final String title, @Param("body") final String body,
        @Param("eventKey") final String eventKey);

    List<Notification> findTop20ByUserIdAndDeletedAtIsNullOrderByIdDesc(final Long userId);

    long countByUserIdAndDeletedAtIsNullAndReadAtIsNull(final Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :notificationId and n.userId = :userId and n.deletedAt is null")
    Optional<Notification> findOwnedActiveForUpdate(@Param("userId") final Long userId, @Param("notificationId") final Long notificationId);
}
