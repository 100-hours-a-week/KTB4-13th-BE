package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.UserOnboardingBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserOnboardingBookJpaRepository extends JpaRepository<UserOnboardingBook, Long> {
    List<UserOnboardingBook> findByUserId(final Long userId);

    void deleteByUserId(final Long userId);
}
