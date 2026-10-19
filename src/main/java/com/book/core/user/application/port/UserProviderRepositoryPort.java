package com.book.core.user.application.port;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserProviderRepositoryPort {
    UserProvider save(final UserProvider userProvider);

    Optional<UserProvider> findActiveByProviderTypeAndProviderUserId(final ProviderType providerType, final String providerUserId);

    List<UserProvider> findActiveByUserId(final Long userId);

    void markUnlinked(final Long userProviderId, final LocalDateTime unlinkedAt);
}
