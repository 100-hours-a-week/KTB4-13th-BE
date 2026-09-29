package com.book.core.onboarding.application.service;

import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.RecordPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.GetPersonalizedRecommendationConsentUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizedRecommendationConsentUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final SaveOnboardingAnswersUseCase saveOnboardingAnswersUseCase;
    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase;
    private final GetPersonalizedRecommendationConsentUseCase getPersonalizedRecommendationConsentUseCase;
    private final RecordPersonalizedRecommendationConsentUseCase recordPersonalizedRecommendationConsentUseCase;
    private final GetOnboardingBookCandidatesUseCase getOnboardingBookCandidatesUseCase;

    public OnboardingQuestionResult getQuestion(final GetOnboardingQuestionCommand command) {
        return getOnboardingQuestionUseCase.execute(command);
    }

    public OnboardingProgressResult getProgress(final GetOnboardingProgressCommand command) {
        return getOnboardingProgressUseCase.execute(command);
    }

    public void saveAnswers(final PutOnboardingAnswersCommand command) {
        saveOnboardingAnswersUseCase.execute(command);
    }

    public void saveBooks(final PutOnboardingBooksCommand command) {
        saveOnboardingBooksUseCase.execute(command);
    }

    public PersonalizedRecommendationConsentResult getConsent(final GetPersonalizedRecommendationConsentCommand command) {
        return getPersonalizedRecommendationConsentUseCase.execute(command);
    }

    public PersonalizedRecommendationConsentResult recordConsent(final RecordPersonalizedRecommendationConsentCommand command) {
        return recordPersonalizedRecommendationConsentUseCase.execute(command);
    }

    public List<OnboardingBookCandidateResult> getBookCandidates() {
        return getOnboardingBookCandidatesUseCase.execute();
    }
}
