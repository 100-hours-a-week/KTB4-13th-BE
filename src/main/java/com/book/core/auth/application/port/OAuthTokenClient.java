package com.book.core.auth.application.port;

import com.book.core.user.domain.ProviderType;

public interface OAuthTokenClient {
    String exchangeForIdToken(
            final ProviderType providerType, final String authorizationCode, final String codeVerifier);
}
