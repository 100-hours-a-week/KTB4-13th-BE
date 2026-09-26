package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.OnboardingOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OnboardingOptionJpaRepository extends JpaRepository<OnboardingOption, Long> {
    List<OnboardingOption> findByOnboardingQuestionIdOrderByDisplayOrderAsc(final Long onboardingQuestionId);

    List<OnboardingOption> findAllByIdIn(final List<Long> ids);
}
