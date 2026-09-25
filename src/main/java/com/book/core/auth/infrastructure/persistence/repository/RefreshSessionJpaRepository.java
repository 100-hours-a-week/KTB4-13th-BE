package com.book.core.auth.infrastructure.persistence.repository;

import com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, Long> {

    Optional<RefreshSessionEntity> findByUserIdAndRevokedAtIsNull(final Long userId);

    Optional<RefreshSessionEntity> findByTokenHashAndRevokedAtIsNull(final String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from RefreshSessionEntity r
            where r.tokenHash = :tokenHash
              and r.revokedAt is null
            """)
    Optional<RefreshSessionEntity> findActiveByTokenHashForUpdate(@Param("tokenHash") final String tokenHash);
}
