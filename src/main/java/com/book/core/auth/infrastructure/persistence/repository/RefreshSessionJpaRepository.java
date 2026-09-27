package com.book.core.auth.infrastructure.persistence.repository;

import com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, Long> {
    Optional<RefreshSessionEntity> findByUserIdAndRevokedAtIsNull(final Long userId);

    Optional<RefreshSessionEntity> findByTokenHashAndRevokedAtIsNull(final String tokenHash);
}
