package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserOnboardingAnswerJpaRepository extends JpaRepository<UserOnboardingAnswer, Long> {
    List<UserOnboardingAnswer> findByUserId(final Long userId);

    void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds);
}
