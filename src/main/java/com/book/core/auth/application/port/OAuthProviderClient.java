package com.book.core.auth.application.port;

public interface OAuthProviderClient {
    OAuthIdentity verify(final String idToken, final String expectedNonce);
}
