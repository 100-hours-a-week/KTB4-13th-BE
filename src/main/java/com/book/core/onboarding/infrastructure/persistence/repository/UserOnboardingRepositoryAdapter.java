package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.UserOnboarding;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserOnboardingRepositoryAdapter implements UserOnboardingRepositoryPort {
    private final UserOnboardingJpaRepository jpaRepository;

    @Override
    public Optional<UserOnboarding> findActiveByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public UserOnboarding save(final UserOnboarding userOnboarding) {
        return jpaRepository.save(userOnboarding);
    }
}
