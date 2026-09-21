package com.book.core.user.infrastructure.persistence.mapper;

import com.book.core.user.domain.User;
import com.book.core.user.infrastructure.persistence.entity.UserEntity;

public final class UserPersistenceMapper {
    private UserPersistenceMapper() {}

    public static UserEntity toEntity(final User user) {
        return new UserEntity(user.id(), user.nickname(), user.deletedAt());
    }

    public static User toDomain(final UserEntity entity) {
        return User.restore(entity.id(), entity.nickname(), entity.deletedAt());
    }
}
