package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.OnboardingBookCandidate;
import java.util.List;

public interface OnboardingBookCandidateRepositoryPort {
    List<OnboardingBookCandidate> findAllOrderByDisplayOrder();
}
