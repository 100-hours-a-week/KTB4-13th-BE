package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.OnboardingBookCandidateRepositoryPort;
import com.book.core.onboarding.domain.OnboardingBookCandidate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class OnboardingBookCandidateRepositoryAdapter implements OnboardingBookCandidateRepositoryPort {
    private final OnboardingBookCandidateJpaRepository jpaRepository;

    @Override
    public List<OnboardingBookCandidate> findAllOrderByDisplayOrder() {
        return jpaRepository.findAllByOrderByDisplayOrderAsc();
    }
}
