package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.result.SaveOnboardingBooksResult;
import com.book.core.onboarding.application.usecase.CreatePersonalizationProfileUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.CannotCreateTransactionException;

class OnboardingServiceTest {
    private static final Long USER_ID = 42L;
    private static final PutOnboardingBooksCommand COMMAND = new PutOnboardingBooksCommand(USER_ID, List.of(10L));
    private static final AddLibraryBooksCommand LIBRARY_COMMAND = new AddLibraryBooksCommand(USER_ID, List.of(10L));

    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase = mock(SaveOnboardingBooksUseCase.class);
    private final CreatePersonalizationProfileUseCase createPersonalizationProfileUseCase = mock(CreatePersonalizationProfileUseCase.class);
    private final GetOnboardingBookCandidatesUseCase getBookCandidatesUseCase = mock(GetOnboardingBookCandidatesUseCase.class);
    private final AddLibraryBooksUseCase addLibraryBooksUseCase = mock(AddLibraryBooksUseCase.class);
    private final OnboardingService onboardingService =
        new OnboardingService(mock(GetOnboardingQuestionUseCase.class), mock(GetOnboardingProgressUseCase.class),
            mock(SaveOnboardingAnswersUseCase.class), saveOnboardingBooksUseCase, getBookCandidatesUseCase,
            mock(RecordPersonalizationAgreementUseCase.class), createPersonalizationProfileUseCase, addLibraryBooksUseCase);

    @BeforeEach
    void completeOnboardingForTheFirstTime() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(true));
    }

    @Test
    void 후보_조회_Command를_UseCase로_전달한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        onboardingService.getBookCandidates(command);
        verify(getBookCandidatesUseCase).execute(command);
        verifyNoInteractions(saveOnboardingBooksUseCase, createPersonalizationProfileUseCase);
    }

    @Test
    void 최초_완료하면_온보딩_저장_도서관_추가_AI_취향_프로필_생성_순서로_실행한다() {
        onboardingService.saveBooks(COMMAND);

        final var ordered = inOrder(saveOnboardingBooksUseCase, addLibraryBooksUseCase, createPersonalizationProfileUseCase);
        ordered.verify(saveOnboardingBooksUseCase).execute(COMMAND);
        ordered.verify(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);
        ordered.verify(createPersonalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 이미_완료한_온보딩의_재요청은_도서관에_추가하지_않고_AI_취향_프로필_생성만_요청한다() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(false));

        onboardingService.saveBooks(COMMAND);

        verifyNoInteractions(addLibraryBooksUseCase);
        verify(createPersonalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 도서관_추가의_일시적인_DB_오류는_최대_3회_시도한_뒤_격리하고_AI_취향_프로필_생성을_요청한다() {
        doThrow(new CannotAcquireLockException("deadlock")).when(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
        verify(addLibraryBooksUseCase, times(3)).execute(LIBRARY_COMMAND);
        verify(createPersonalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 도서관_추가가_동시_추가로_UNIQUE_충돌하면_재시도에서_성공한다() {
        doThrow(new DuplicateKeyException("duplicate")).doNothing().when(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);

        onboardingService.saveBooks(COMMAND);

        verify(addLibraryBooksUseCase, times(2)).execute(LIBRARY_COMMAND);
    }

    @Test
    void 도서관_추가의_일시적이지_않은_DB_오류는_재시도하지_않고_격리한다() {
        doThrow(new DataIntegrityViolationException("fk")).when(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
        verify(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);
        verify(createPersonalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 온보딩_도서_저장이_실패하면_AI를_호출하지_않는다() {
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_FOUND)).when(saveOnboardingBooksUseCase).execute(COMMAND);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(CoreException.class);
        verifyNoInteractions(addLibraryBooksUseCase, createPersonalizationProfileUseCase);
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
