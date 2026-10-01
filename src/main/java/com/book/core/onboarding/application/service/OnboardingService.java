package com.book.core.onboarding.application.service;

import com.book.common.exception.CoreException;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.usecase.CreatePersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final SaveOnboardingAnswersUseCase saveOnboardingAnswersUseCase;
    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase;
    private final GetOnboardingBookCandidatesUseCase getOnboardingBookCandidatesUseCase;
    private final RecordPersonalizationAgreementUseCase recordPersonalizationAgreementUseCase;
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
        createPersonalizationProfile(command.userId());
    }

    public List<OnboardingBookCandidateResult> getBookCandidates(final GetOnboardingBookCandidatesCommand command) {
        return getOnboardingBookCandidatesUseCase.execute(command);
    }

    public void recordPersonalizationAgreement(final RecordPersonalizationAgreementCommand command) {
        recordPersonalizationAgreementUseCase.execute(command);
    }

    private void createPersonalizationProfile(final Long userId) {
        try {
            createPersonalizationProfileUseCase.execute(userId);
        } catch (final CoreException | DataAccessException | TransactionException exception) {
            log.warn("AI 취향 프로필 생성에 실패했습니다. userId={}", userId, exception);
        }
    }
}
