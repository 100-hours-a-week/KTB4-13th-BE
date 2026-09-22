package com.book.core.auth.application.port;

import com.book.core.auth.domain.RefreshSession;
import java.util.Optional;

public interface RefreshSessionRepository {
    RefreshSession save(final RefreshSession refreshSession);

    Optional<RefreshSession> findActiveByUserId(final Long userId);

    Optional<RefreshSession> findActiveByTokenHash(final String tokenHash);

    Optional<RefreshSession> findActiveByTokenHashForUpdate(final String tokenHash);
}
