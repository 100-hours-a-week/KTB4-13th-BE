package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.List;

public interface UserOnboardingAnswerRepositoryPort {
    List<UserOnboardingAnswer> findByUserId(final Long userId);

    void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds);

    List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> answers);
}
