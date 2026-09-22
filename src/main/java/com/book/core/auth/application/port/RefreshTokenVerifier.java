package com.book.core.auth.application.port;

public interface RefreshTokenVerifier {

    Long verify(final String refreshToken);
}
