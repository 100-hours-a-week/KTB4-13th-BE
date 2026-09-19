package com.book.core.auth.infrastructure.persistence.mapper;

import com.book.core.auth.domain.RefreshSession;
import com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity;

public final class RefreshSessionPersistenceMapper {
    private RefreshSessionPersistenceMapper() {}

    public static RefreshSessionEntity toEntity(final RefreshSession refreshSession) {
        return new RefreshSessionEntity(
                refreshSession.id(),
                refreshSession.userId(),
                refreshSession.tokenHash(),
                refreshSession.expiresAt(),
                refreshSession.revokedAt());
    }

    public static RefreshSession toDomain(final RefreshSessionEntity entity) {
        return RefreshSession.restore(
                entity.id(), entity.userId(), entity.tokenHash(), entity.expiresAt(), entity.revokedAt());
    }
}
