package com.book.core.user.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.BusinessException;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class User extends BaseEntity {
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "delete_requested_at")
    private LocalDateTime deleteRequestedAt;

    private User(final Long id, final String nickname) {
        super(id);
        this.nickname = normalizeNickname(nickname);
    }

    public static User create(final String nickname) {
        return new User(null, nickname);
    }

    public static User restore(final Long id, final String nickname) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID);
        }
        return new User(id, nickname);
    }

    public void changeNickname(final String nickname) {
        final String normalized = normalizeNickname(nickname);
        if (!normalized.equals(this.nickname)) {
            this.nickname = normalized;
        }
    }

    public void requestWithdrawal(final LocalDateTime requestedAt) {
        if (deleteRequestedAt == null) {
            deleteRequestedAt = requestedAt;
        }
    }

    public boolean isWithdrawalRequested() {
        return deleteRequestedAt != null;
    }

    public static String normalizeNickname(final String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }
        final String normalized = nickname.strip();
        if (normalized.length() < MIN_NICKNAME_LENGTH || normalized.length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }
        return normalized;
    }
}
