package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.infrastructure.persistence.entity.UserProviderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserProviderJpaRepository extends JpaRepository<UserProviderEntity, Long> {
    Optional<UserProviderEntity> findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(
            final ProviderType providerType, final String providerUserId);
}
