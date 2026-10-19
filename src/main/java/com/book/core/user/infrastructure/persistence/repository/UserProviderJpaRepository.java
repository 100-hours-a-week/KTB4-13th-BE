package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface UserProviderJpaRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderTypeAndProviderUserIdAndDeletedAtIsNull(final ProviderType providerType,
        final String providerUserId);

    @Transactional
    @Modifying
    @Query("update UserProvider p set p.unlinkedAt = :unlinkedAt where p.id = :id and p.unlinkedAt is null and p.deletedAt is null")
    int markUnlinked(@Param("id") final Long id, @Param("unlinkedAt") final LocalDateTime unlinkedAt);
}
