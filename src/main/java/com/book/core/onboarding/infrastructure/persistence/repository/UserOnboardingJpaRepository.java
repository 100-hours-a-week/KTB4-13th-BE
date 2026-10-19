package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboarding;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserOnboardingJpaRepository extends JpaRepository<UserOnboarding, Long> {
    Optional<UserOnboarding> findByUserIdAndDeletedAtIsNull(final Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select onboarding from UserOnboarding onboarding where onboarding.userId = :userId and onboarding.deletedAt is null")
    Optional<UserOnboarding> findActiveByUserIdForUpdate(@Param("userId") final Long userId);
}
