package com.book.core.user.domain;

import com.book.common.exception.BusinessException;
import com.book.core.user.domain.exception.UserErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class User {
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private User(final Long id, final String nickname, final LocalDateTime deletedAt) {
        this.id = id;
        this.nickname = normalizeNickname(nickname);
        this.deletedAt = deletedAt;
    }

    public static User create(final String nickname) {
        return new User(null, nickname, null);
    }

    public static User restore(final Long id, final String nickname, final LocalDateTime deletedAt) {
        if (id == null || id <= 0) {
            throw new BusinessException(UserErrorCode.INVALID_USER_ID);
        }
        return new User(id, nickname, deletedAt);
    }

    public static String normalizeNickname(final String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
        }
        final String normalized = nickname.strip();
        if (normalized.length() < MIN_NICKNAME_LENGTH || normalized.length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
        }
        return normalized;
    }

    public boolean isActive() {
        return deletedAt == null;
    }
}
