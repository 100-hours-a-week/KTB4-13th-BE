package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OnboardingQuestionJpaRepository extends JpaRepository<OnboardingQuestion, Long> {
    Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThanOrderByDisplayOrderAsc(final int displayOrder);
}
