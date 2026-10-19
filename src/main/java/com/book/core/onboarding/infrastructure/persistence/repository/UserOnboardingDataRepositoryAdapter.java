package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserOnboardingDataRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserOnboardingDataRepositoryAdapter implements UserOnboardingDataRepositoryPort {
    private final UserOnboardingAnswerJpaRepository userOnboardingAnswerJpaRepository;
    private final UserOnboardingBookJpaRepository userOnboardingBookJpaRepository;
    private final UserOnboardingJpaRepository userOnboardingJpaRepository;

    @Override
    public void deleteAllByUserId(final Long userId) {
        userOnboardingAnswerJpaRepository.deleteAllByUserId(userId);
        userOnboardingBookJpaRepository.deleteByUserId(userId);
        userOnboardingJpaRepository.deleteAllByUserId(userId);
    }
}
