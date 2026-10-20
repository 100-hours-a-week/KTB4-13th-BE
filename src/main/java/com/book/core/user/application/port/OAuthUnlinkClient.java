package com.book.core.user.application.port;

import com.book.core.user.domain.ProviderType;

public interface OAuthUnlinkClient {
    void unlink(final ProviderType providerType, final String providerUserId);
}
