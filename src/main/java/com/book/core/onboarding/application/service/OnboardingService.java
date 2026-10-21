package com.book.core.onboarding.application.service;

import com.book.common.exception.CoreException;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.result.SaveOnboardingBooksResult;
import com.book.core.onboarding.application.usecase.PersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import com.book.core.onboarding.application.usecase.UpdateOnboardingPreferencesUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {
    private static final int LIBRARY_ADD_MAX_ATTEMPTS = 3;

    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase;
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase;
    private final SaveOnboardingAnswersUseCase saveOnboardingAnswersUseCase;
    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase;
    private final GetOnboardingBookCandidatesUseCase getOnboardingBookCandidatesUseCase;
    private final RecordPersonalizationAgreementUseCase recordPersonalizationAgreementUseCase;
    private final PersonalizationProfileUseCase personalizationProfileUseCase;
    private final UpdateOnboardingPreferencesUseCase updateOnboardingPreferencesUseCase;
    private final AddLibraryBooksUseCase addLibraryBooksUseCase;

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
        final SaveOnboardingBooksResult result = saveOnboardingBooksUseCase.execute(command);
        if (result.firstCompleted()) {
            addLibraryBooks(command.userId(), command.bookIds());
        }
        createPersonalizationProfile(command.userId());
    }

    public void updatePreferences(final PutOnboardingPreferencesCommand command) {
        if (updateOnboardingPreferencesUseCase.execute(command)) {
            updatePersonalizationProfile(command.userId());
        }
    }

    public List<OnboardingBookCandidateResult> getBookCandidates(final GetOnboardingBookCandidatesCommand command) {
        return getOnboardingBookCandidatesUseCase.execute(command);
    }

    public void recordPersonalizationAgreement(final RecordPersonalizationAgreementCommand command) {
        recordPersonalizationAgreementUseCase.execute(command);
    }

    /** 커밋된 온보딩과 별도 트랜잭션으로 추가하며, 일시적인 DB 오류만 재시도하고 최종 실패는 온보딩 완료에 영향을 주지 않습니다. */
    private void addLibraryBooks(final Long userId, final List<Long> bookIds) {
        for (int attempt = 1; attempt <= LIBRARY_ADD_MAX_ATTEMPTS; attempt++) {
            try {
                addLibraryBooksUseCase.execute(new AddLibraryBooksCommand(userId, bookIds));
                return;
            } catch (final TransientDataAccessException | DuplicateKeyException exception) {
                if (attempt == LIBRARY_ADD_MAX_ATTEMPTS) {
                    log.atError().addKeyValue("userId", userId).log("[ONBOARDING] 온보딩 도서의 나의 도서관 추가에 실패했습니다. userId={}, attempts={}",
                        userId, attempt, exception);
                }
            } catch (final CoreException exception) {
                log.atError().addKeyValue("userId", userId).log(
                    "[ONBOARDING] 온보딩 도서의 나의 도서관 추가에 실패했습니다. userId={}, attempts={}, errorCode={}", userId, attempt,
                    exception.errorCode().name());
                return;
            } catch (final DataAccessException | TransactionException exception) {
                log.atError().addKeyValue("userId", userId).log("[ONBOARDING] 온보딩 도서의 나의 도서관 추가에 실패했습니다. userId={}, attempts={}", userId,
                    attempt, exception);
                return;
            }
        }
    }

    private void createPersonalizationProfile(final Long userId) {
        try {
            personalizationProfileUseCase.execute(userId);
        } catch (final CoreException exception) {
            log.atWarn().addKeyValue("userId", userId).log("[ONBOARDING] AI 취향 프로필 생성에 실패했습니다. userId={}, errorCode={}", userId,
                exception.errorCode().name());
        } catch (final DataAccessException | TransactionException exception) {
            log.atWarn().addKeyValue("userId", userId).log("[ONBOARDING] AI 취향 프로필 생성에 실패했습니다. userId={}", userId, exception);
        }
    }

    private void updatePersonalizationProfile(final Long userId) {
        try {
            personalizationProfileUseCase.executeUpdate(userId);
        } catch (final CoreException exception) {
            log.atWarn().addKeyValue("userId", userId).log("[ONBOARDING] AI 취향 프로필 갱신에 실패했습니다. userId={}, errorCode={}", userId,
                exception.errorCode().name());
        } catch (final DataAccessException | TransactionException exception) {
            log.atWarn().addKeyValue("userId", userId).log("[ONBOARDING] AI 취향 프로필 갱신에 실패했습니다. userId={}", userId, exception);
        }
    }
}
