package com.book.core.notification.infrastructure.persistence.repository;

import com.book.core.notification.domain.Notification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Modifying
    @Query("DELETE FROM Notification notification WHERE notification.userId = :userId")
    void deleteAllByUserId(@Param("userId") final Long userId);

    @Modifying
    @Query("DELETE FROM Notification notification WHERE notification.createdAt < :createdAt")
    void deleteCreatedBefore(@Param("createdAt") final LocalDateTime createdAt);
}
