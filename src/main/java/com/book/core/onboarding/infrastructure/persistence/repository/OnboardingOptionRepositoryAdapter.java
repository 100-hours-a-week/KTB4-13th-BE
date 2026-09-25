package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class OnboardingOptionRepositoryAdapter implements OnboardingOptionRepositoryPort {
    private final OnboardingOptionJpaRepository jpaRepository;

    @Override
    public List<OnboardingOption> findByQuestionId(final Long questionId) {
        return jpaRepository.findByOnboardingQuestionIdOrderByDisplayOrderAsc(questionId);
    }

    @Override
    public List<OnboardingOption> findByQuestionIdAndParentOptionIdIn(
            final Long questionId, final List<Long> parentOptionIds) {
        return jpaRepository.findByOnboardingQuestionIdAndParentOptionIdInOrderByDisplayOrderAsc(
                questionId, parentOptionIds);
    }

    @Override
    public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }
}
