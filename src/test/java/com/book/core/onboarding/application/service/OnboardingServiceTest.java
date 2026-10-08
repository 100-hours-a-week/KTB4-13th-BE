package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.usecase.CreatePersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

class OnboardingServiceTest {
    private static final Long USER_ID = 42L;
    private static final PutOnboardingBooksCommand COMMAND = new PutOnboardingBooksCommand(USER_ID, List.of(10L));

    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase = mock(SaveOnboardingBooksUseCase.class);
    private final CreatePersonalizationProfileUseCase createPersonalizationProfileUseCase = mock(CreatePersonalizationProfileUseCase.class);
    private final GetOnboardingBookCandidatesUseCase getBookCandidatesUseCase = mock(GetOnboardingBookCandidatesUseCase.class);
    private final OnboardingService onboardingService = new OnboardingService(mock(GetOnboardingQuestionUseCase.class),
        mock(GetOnboardingProgressUseCase.class), mock(SaveOnboardingAnswersUseCase.class), saveOnboardingBooksUseCase,
        getBookCandidatesUseCase, mock(RecordPersonalizationAgreementUseCase.class), createPersonalizationProfileUseCase);

    @Test
    void 후보_조회_Command를_UseCase로_전달한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        onboardingService.getBookCandidates(command);
        verify(getBookCandidatesUseCase).execute(command);
        verifyNoInteractions(saveOnboardingBooksUseCase, createPersonalizationProfileUseCase);
    }

    @Test
    void 온보딩_도서_저장이_끝난_뒤_AI_취향_프로필_생성을_요청한다() {
        onboardingService.saveBooks(COMMAND);

        final var ordered = inOrder(saveOnboardingBooksUseCase, createPersonalizationProfileUseCase);
        ordered.verify(saveOnboardingBooksUseCase).execute(COMMAND);
        ordered.verify(createPersonalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 온보딩_도서_저장이_실패하면_AI를_호출하지_않는다() {
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_FOUND)).when(saveOnboardingBooksUseCase).execute(COMMAND);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(CoreException.class);
        verifyNoInteractions(createPersonalizationProfileUseCase);
    }

    @Test
    void AI_취향_프로필_생성이_실패해도_온보딩_완료는_성공한다() {
        doThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE)).when(createPersonalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_취향_프로필_생성_중_DB_조회가_실패해도_온보딩_완료는_성공한다() {
        doThrow(new DataAccessResourceFailureException("db down")).when(createPersonalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_취향_프로필_생성_중_DB_연결을_얻지_못해도_온보딩_완료는_성공한다() {
        doThrow(new CannotCreateTransactionException("no connection")).when(createPersonalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_실패가_아닌_예외는_숨기지_않는다() {
        doThrow(new IllegalStateException("bug")).when(createPersonalizationProfileUseCase).execute(USER_ID);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(IllegalStateException.class);
    }
}
