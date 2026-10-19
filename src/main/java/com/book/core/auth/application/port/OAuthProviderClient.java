package com.book.core.auth.application.port;

import com.book.core.auth.application.result.OAuthUserInfo;

public interface OAuthProviderClient {
    OAuthUserInfo verify(final String idToken, final String expectedNonce);
}
