package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.infrastructure.persistence.entity.UserProviderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserProviderJpaRepository extends JpaRepository<UserProviderEntity, Long> {
    @Query(value = """
                    SELECT id, user_id, provider_type, provider_user_id, provider_email, deleted_at
                    FROM user_providers
                    WHERE provider_type = :providerType
                      AND provider_user_id = :providerUserId
                      AND active_flag = 1
                    """, nativeQuery = true)
    Optional<UserProviderEntity> findActiveByProviderTypeAndProviderUserId(
            @Param("providerType") final String providerType, @Param("providerUserId") final String providerUserId);
}
