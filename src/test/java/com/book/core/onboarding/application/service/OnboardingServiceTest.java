package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.usecase.CreatePersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.GetPersonalizedRecommendationConsentUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizedRecommendationConsentUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingServiceTest {
    private final GetOnboardingQuestionUseCase getOnboardingQuestionUseCase = mock(GetOnboardingQuestionUseCase.class);
    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase = mock(GetOnboardingProgressUseCase.class);
    private final SaveOnboardingAnswersUseCase saveOnboardingAnswersUseCase = mock(SaveOnboardingAnswersUseCase.class);
    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase = mock(SaveOnboardingBooksUseCase.class);
    private final GetPersonalizedRecommendationConsentUseCase getPersonalizedRecommendationConsentUseCase =
        mock(GetPersonalizedRecommendationConsentUseCase.class);
    private final RecordPersonalizedRecommendationConsentUseCase recordPersonalizedRecommendationConsentUseCase =
        mock(RecordPersonalizedRecommendationConsentUseCase.class);
    private final GetOnboardingBookCandidatesUseCase getOnboardingBookCandidatesUseCase = mock(GetOnboardingBookCandidatesUseCase.class);
    private final CreatePersonalizationProfileUseCase createPersonalizationProfileUseCase = mock(CreatePersonalizationProfileUseCase.class);

    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(getOnboardingQuestionUseCase, getOnboardingProgressUseCase, saveOnboardingAnswersUseCase,
            saveOnboardingBooksUseCase, getPersonalizedRecommendationConsentUseCase, recordPersonalizedRecommendationConsentUseCase,
            getOnboardingBookCandidatesUseCase, createPersonalizationProfileUseCase);
    }

    @Test
    void 도서_저장_후_개인화_프로필_생성을_시도한다() {
        final var command = new PutOnboardingBooksCommand(42L, List.of(10L));

        onboardingService.saveBooks(command);

        final var ordered = inOrder(saveOnboardingBooksUseCase, createPersonalizationProfileUseCase);
        ordered.verify(saveOnboardingBooksUseCase).execute(command);
        ordered.verify(createPersonalizationProfileUseCase).execute(42L);
    }

    @Test
    void 같은_요청을_재호출하면_프로필_생성을_다시_시도한다() {
        final var command = new PutOnboardingBooksCommand(42L, List.of(10L));

        onboardingService.saveBooks(command);
        onboardingService.saveBooks(command);

        verify(saveOnboardingBooksUseCase, times(2)).execute(command);
        verify(createPersonalizationProfileUseCase, times(2)).execute(42L);
    }

    @Test
    void 프로필_생성이_실패해도_온보딩_완료_자체는_성공한다() {
        final var command = new PutOnboardingBooksCommand(42L, List.of(10L));
        doThrow(new CoreException(ErrorCode.AI_PROFILE_FAILURE)).when(createPersonalizationProfileUseCase).execute(42L);

        assertThatCode(() -> onboardingService.saveBooks(command)).doesNotThrowAnyException();

        verify(saveOnboardingBooksUseCase).execute(command);
    }

    @Test
    void 도서_저장_자체가_실패하면_프로필_생성을_시도하지_않는다() {
        final var command = new PutOnboardingBooksCommand(42L, List.of(10L));
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_FOUND)).when(saveOnboardingBooksUseCase).execute(command);

        org.junit.jupiter.api.Assertions.assertThrows(CoreException.class, () -> onboardingService.saveBooks(command));

        org.mockito.Mockito.verifyNoInteractions(createPersonalizationProfileUseCase);
    }
}
