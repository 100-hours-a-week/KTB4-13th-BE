package com.book.core.user.application.port;

public interface RefreshSessionRevokerPort {
    void revokeAll(final Long userId);
}
