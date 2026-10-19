package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboardingAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserOnboardingAnswerJpaRepository extends JpaRepository<UserOnboardingAnswer, Long> {
    List<UserOnboardingAnswer> findByUserId(final Long userId);

    void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserOnboardingAnswer e where e.userId = :userId")
    void deleteAllByUserId(@Param("userId") final Long userId);
}
