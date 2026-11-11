package com.book.core.auth.application.port;

import java.time.Instant;

public interface RefreshSessionRepository {
    void saveLoginSession(Long userId, String tokenHash, Instant expiresAt);

    boolean rotate(Long userId, String expectedHash, String newHash, Instant expiresAt);

    void revoke(Long userId);
}
