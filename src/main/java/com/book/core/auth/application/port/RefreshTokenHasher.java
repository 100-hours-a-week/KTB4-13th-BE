package com.book.core.auth.application.port;

public interface RefreshTokenHasher {
    String hash(final String refreshToken);
}
