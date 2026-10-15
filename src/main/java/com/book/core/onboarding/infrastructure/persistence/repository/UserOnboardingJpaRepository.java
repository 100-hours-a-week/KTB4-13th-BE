package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboarding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserOnboardingJpaRepository extends JpaRepository<UserOnboarding, Long> {
    Optional<UserOnboarding> findByUserIdAndDeletedAtIsNull(final Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserOnboarding e where e.userId = :userId")
    void deleteAllByUserId(@Param("userId") final Long userId);
}
