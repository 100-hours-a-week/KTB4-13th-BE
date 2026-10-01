package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.domain.OnboardingBookCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OnboardingBookCandidateJpaRepository extends JpaRepository<OnboardingBookCandidate, Long> {
    List<OnboardingBookCandidate> findAllByOrderByDisplayOrderAsc();

    List<OnboardingBookCandidate> findBySubcategoryCodeInOrderByDisplayOrderAscBookIdAsc(final List<String> subcategoryCodes);
}
