package com.book.core.user.domain;

import com.book.common.exception.BusinessException;
import com.book.core.user.domain.exception.UserErrorCode;
import java.time.LocalDateTime;

public final class UserProvider {
    private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;
    private static final int MAX_PROVIDER_EMAIL_LENGTH = 254;

    private final Long id;
    private final Long userId;
    private final ProviderType providerType;
    private final String providerUserId;
    private final String providerEmail;
    private final LocalDateTime deletedAt;

    private UserProvider(
            final Long id,
            final Long userId,
            final ProviderType providerType,
            final String providerUserId,
            final String providerEmail,
            final LocalDateTime deletedAt) {
        validateUserId(userId);
        validateProviderType(providerType);
        validateProviderUserId(providerUserId);
        validateProviderEmail(providerEmail);
        this.id = id;
        this.userId = userId;
        this.providerType = providerType;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.deletedAt = deletedAt;
    }

    public static UserProvider create(
            final Long userId,
            final ProviderType providerType,
            final String providerUserId,
            final String providerEmail) {
        return new UserProvider(null, userId, providerType, providerUserId, providerEmail, null);
    }

    public static UserProvider restore(
            final Long id,
            final Long userId,
            final ProviderType providerType,
            final String providerUserId,
            final String providerEmail,
            final LocalDateTime deletedAt) {
        if (id == null || id <= 0) {
            throw new BusinessException(UserErrorCode.INVALID_USER_PROVIDER_ID);
        }
        return new UserProvider(id, userId, providerType, providerUserId, providerEmail, deletedAt);
    }

    private static void validateUserId(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(UserErrorCode.INVALID_USER_ID);
        }
    }

    private static void validateProviderType(final ProviderType providerType) {
        if (providerType == null) {
            throw new BusinessException(UserErrorCode.INVALID_PROVIDER_TYPE);
        }
    }

    private static void validateProviderUserId(final String providerUserId) {
        if (providerUserId == null
                || providerUserId.isBlank()
                || providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROVIDER_USER_ID);
        }
    }

    private static void validateProviderEmail(final String providerEmail) {
        if (providerEmail != null && providerEmail.length() > MAX_PROVIDER_EMAIL_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROVIDER_EMAIL);
        }
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public ProviderType providerType() {
        return providerType;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String providerEmail() {
        return providerEmail;
    }

    public LocalDateTime deletedAt() {
        return deletedAt;
    }
}
