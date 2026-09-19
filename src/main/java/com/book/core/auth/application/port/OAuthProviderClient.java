package com.book.core.auth.application.port;

import com.book.core.user.domain.ProviderType;

public interface OAuthProviderClient {
    OAuthIdentity verify(final ProviderType providerType, final String idToken);
}
