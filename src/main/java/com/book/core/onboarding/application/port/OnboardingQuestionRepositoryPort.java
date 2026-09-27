package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.Optional;

public interface OnboardingQuestionRepositoryPort {
    Optional<OnboardingQuestion> findById(final Long id);

    Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThan(final int displayOrder);
}
