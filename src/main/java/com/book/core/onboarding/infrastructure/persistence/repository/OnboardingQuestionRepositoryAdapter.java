package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class OnboardingQuestionRepositoryAdapter implements OnboardingQuestionRepositoryPort {
    private final OnboardingQuestionJpaRepository jpaRepository;

    @Override
    public Optional<OnboardingQuestion> findById(final Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThan(final int displayOrder) {
        return jpaRepository.findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(displayOrder);
    }
}
