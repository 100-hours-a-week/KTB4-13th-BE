package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserOnboardingAnswerRepositoryAdapter implements UserOnboardingAnswerRepositoryPort {
    private final UserOnboardingAnswerJpaRepository jpaRepository;

    @Override
    public List<UserOnboardingAnswer> findByUserId(final Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {
        jpaRepository.deleteByUserIdAndOnboardingOptionIdIn(userId, onboardingOptionIds);
    }

    @Override
    public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers) {
        return jpaRepository.saveAll(answers);
    }
}
