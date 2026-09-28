package com.book.core.auth.application.port;

public interface OAuthTokenClient {
    String exchangeForIdToken(final String authorizationCode, final String codeVerifier);
}
