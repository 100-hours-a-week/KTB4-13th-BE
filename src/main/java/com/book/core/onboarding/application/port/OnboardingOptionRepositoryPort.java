package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.OnboardingOption;
import java.util.List;

public interface OnboardingOptionRepositoryPort {
    List<OnboardingOption> findByQuestionId(final Long questionId);

    List<OnboardingOption> findAllByIdIn(final List<Long> ids);
}
