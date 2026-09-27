package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboarding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserOnboardingJpaRepository extends JpaRepository<UserOnboarding, Long> {
    Optional<UserOnboarding> findByUserIdAndDeletedAtIsNull(final Long userId);
}
