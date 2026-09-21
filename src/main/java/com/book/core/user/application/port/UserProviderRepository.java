package com.book.core.user.application.port;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.util.Optional;

public interface UserProviderRepository {
    UserProvider save(final UserProvider userProvider);

    Optional<UserProvider> findActiveByProviderTypeAndProviderUserId(
            final ProviderType providerType, final String providerUserId);
}
