package com.book.core.auth.domain;

import java.time.Instant;

public final class RefreshSession {
    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshSession(
            final Long id,
            final Long userId,
            final String tokenHash,
            final Instant expiresAt,
            final Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshSession create(final Long userId, final String tokenHash, final Instant expiresAt) {
        return new RefreshSession(null, userId, tokenHash, expiresAt, null);
    }

    public static RefreshSession restore(
            final Long id,
            final Long userId,
            final String tokenHash,
            final Instant expiresAt,
            final Instant revokedAt) {
        return new RefreshSession(id, userId, tokenHash, expiresAt, revokedAt);
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public void revoke(final Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
