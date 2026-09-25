package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserProviderJpaRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(
            final ProviderType providerType, final String providerUserId);
}
