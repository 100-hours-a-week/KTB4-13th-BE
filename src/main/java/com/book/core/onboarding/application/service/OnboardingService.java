package com.book.core.onboarding.application.service;

import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.application.usecase.CreatePersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.GetPersonalizedRecommendationConsentUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import com.book.core.onboarding.application.usecase.UpdatePersonalizedRecommendationConsentUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final SaveOnboardingAnswersUseCase saveOnboardingAnswersUseCase;
    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase;
    private final GetPersonalizedRecommendationConsentUseCase getPersonalizedRecommendationConsentUseCase;
    private final UpdatePersonalizedRecommendationConsentUseCase updatePersonalizedRecommendationConsentUseCase;
    private final GetOnboardingBookCandidatesUseCase getOnboardingBookCandidatesUseCase;
    private final CreatePersonalizationProfileUseCase createPersonalizationProfileUseCase;

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
        tryCreatePersonalizationProfile(command.userId());
    }

    public PersonalizedRecommendationConsentResult getConsent(final GetPersonalizedRecommendationConsentCommand command) {
        return getPersonalizedRecommendationConsentUseCase.execute(command);
    }

    public PersonalizedRecommendationConsentResult updateConsent(final UpdatePersonalizedRecommendationConsentCommand command) {
        return updatePersonalizedRecommendationConsentUseCase.execute(command);
    }

    public List<OnboardingBookCandidateResult> getBookCandidates() {
        return getOnboardingBookCandidatesUseCase.execute();
    }

    /**
     * 별도 Spring bean인 SaveOnboardingBooksUseCase의 public @Transactional 메서드가 반환된 뒤 호출한다. 따라서 온보딩 저장
     * 트랜잭션은 종료된 상태이며, AI 실패는 이미 완료된 온보딩 결과에 영향을 주지 않는다.
     */
    private void tryCreatePersonalizationProfile(final Long userId) {
        try {
            createPersonalizationProfileUseCase.execute(userId);
        } catch (final RuntimeException exception) {
            log.warn("개인화 추천 프로필 생성에 실패했습니다. userId={}", userId, exception);
        }
    }
}
