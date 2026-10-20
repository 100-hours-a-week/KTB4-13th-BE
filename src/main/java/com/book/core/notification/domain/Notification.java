package com.book.core.notification.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Notification extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, length = 1000)
    private String body;
    @Column(name = "event_key", length = 150)
    private String eventKey;
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification(final Long userId, final String title, final String body) {
        this(userId, title, body, null);
    }

    public Notification(final Long userId, final String title, final String body, final String eventKey) {
        if (userId == null || userId <= 0 || title == null || title.isBlank() || title.length() > 100 || body == null || body.isBlank()
            || body.length() > 1000 || (eventKey != null && (eventKey.isBlank() || eventKey.length() > 150))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.eventKey = eventKey;
    }
}
