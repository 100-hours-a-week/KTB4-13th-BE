package com.book.core.user.infrastructure.persistence.mapper;

import com.book.core.user.domain.UserProvider;
import com.book.core.user.infrastructure.persistence.entity.UserProviderEntity;

public final class UserProviderPersistenceMapper {
    private UserProviderPersistenceMapper() {}

    public static UserProviderEntity toEntity(final UserProvider userProvider) {
        return new UserProviderEntity(
                userProvider.id(),
                userProvider.userId(),
                userProvider.providerType(),
                userProvider.providerUserId(),
                userProvider.providerEmail(),
                userProvider.deletedAt());
    }

    public static UserProvider toDomain(final UserProviderEntity entity) {
        return UserProvider.restore(
                entity.id(),
                entity.userId(),
                entity.providerType(),
                entity.providerUserId(),
                entity.providerEmail(),
                entity.deletedAt());
    }
}
