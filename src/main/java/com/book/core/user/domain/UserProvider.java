package com.book.core.user.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "user_providers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserProvider extends BaseEntity {
    private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;
    private static final int MAX_PROVIDER_EMAIL_LENGTH = 254;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 254)
    private String providerEmail;

    private UserProvider(final Long id, final Long userId, final ProviderType providerType, final String providerUserId,
        final String providerEmail) {
        super(id);
        validateUserId(userId);
        validateProviderType(providerType);
        validateProviderUserId(providerUserId);
        validateProviderEmail(providerEmail);
        this.userId = userId;
        this.providerType = providerType;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
    }

    public static UserProvider create(final Long userId, final ProviderType providerType, final String providerUserId,
        final String providerEmail) {
        return new UserProvider(null, userId, providerType, providerUserId, providerEmail);
    }

    public static UserProvider restore(final Long id, final Long userId, final ProviderType providerType, final String providerUserId,
        final String providerEmail) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.INVALID_USER_PROVIDER_ID);
        }
        return new UserProvider(id, userId, providerType, providerUserId, providerEmail);
    }

    private static void validateUserId(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID);
        }
    }

    private static void validateProviderType(final ProviderType providerType) {
        if (providerType == null) {
            throw new BusinessException(ErrorCode.INVALID_PROVIDER_TYPE);
        }
    }

    private static void validateProviderUserId(final String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank() || providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PROVIDER_USER_ID);
        }
    }

    private static void validateProviderEmail(final String providerEmail) {
        if (providerEmail != null && providerEmail.length() > MAX_PROVIDER_EMAIL_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PROVIDER_EMAIL);
        }
    }
}
