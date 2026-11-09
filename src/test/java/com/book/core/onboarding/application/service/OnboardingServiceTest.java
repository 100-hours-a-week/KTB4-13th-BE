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
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.result.SaveOnboardingBooksResult;
import com.book.core.onboarding.application.usecase.GetOnboardingBookCandidatesUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingQuestionUseCase;
import com.book.core.onboarding.application.usecase.RecordPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import com.book.core.onboarding.application.usecase.UpdateOnboardingPreferencesUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class OnboardingServiceTest {
    private static final Long USER_ID = 42L;
    private static final PutOnboardingBooksCommand COMMAND = new PutOnboardingBooksCommand(USER_ID, List.of(10L));
    private static final PutOnboardingPreferencesCommand PREFERENCES_COMMAND =
        new PutOnboardingPreferencesCommand(USER_ID, List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L))), List.of(10L));
    private static final AddLibraryBooksCommand LIBRARY_COMMAND = new AddLibraryBooksCommand(USER_ID, List.of(10L));

    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase = mock(SaveOnboardingBooksUseCase.class);
    private final GetOnboardingBookCandidatesUseCase getBookCandidatesUseCase = mock(GetOnboardingBookCandidatesUseCase.class);
    private final UpdateOnboardingPreferencesUseCase updateOnboardingPreferencesUseCase = mock(UpdateOnboardingPreferencesUseCase.class);
    private final AddLibraryBooksUseCase addLibraryBooksUseCase = mock(AddLibraryBooksUseCase.class);
    private final OnboardingService onboardingService =
        new OnboardingService(mock(GetOnboardingQuestionUseCase.class), mock(GetOnboardingProgressUseCase.class),
            mock(SaveOnboardingAnswersUseCase.class), saveOnboardingBooksUseCase, getBookCandidatesUseCase,
            mock(RecordPersonalizationAgreementUseCase.class), updateOnboardingPreferencesUseCase, addLibraryBooksUseCase);

    @BeforeEach
    void completeOnboardingForTheFirstTime() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(true));
    }

    @Test
    void 후보_조회_Command를_UseCase로_전달한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        onboardingService.getBookCandidates(command);
        verify(getBookCandidatesUseCase).execute(command);
        verifyNoInteractions(saveOnboardingBooksUseCase);
    }

    @Test
    void 취향_정보_수정_Command를_UseCase로_전달한다() {
        onboardingService.updatePreferences(PREFERENCES_COMMAND);
        verify(updateOnboardingPreferencesUseCase).execute(PREFERENCES_COMMAND);
    }

    @Test
    void 최초_완료하면_온보딩_저장_도서관_추가_순서로_실행한다() {
        onboardingService.saveBooks(COMMAND);

        final var ordered = inOrder(saveOnboardingBooksUseCase, addLibraryBooksUseCase);
        ordered.verify(saveOnboardingBooksUseCase).execute(COMMAND);
        ordered.verify(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);
    }

    @Test
    void 이미_완료한_온보딩의_재요청은_도서관에_추가하지_않는다() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(false));

        onboardingService.saveBooks(COMMAND);

        verifyNoInteractions(addLibraryBooksUseCase);
    }

    @Test
    void 도서관_추가의_일시적인_DB_오류는_최대_3회_시도한_뒤_격리한다() {
        doThrow(new CannotAcquireLockException("deadlock")).when(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
        verify(addLibraryBooksUseCase, times(3)).execute(LIBRARY_COMMAND);
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
    }

    @Test
    void 온보딩_도서_저장이_실패하면_도서관을_변경하지_않는다() {
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_FOUND)).when(saveOnboardingBooksUseCase).execute(COMMAND);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(CoreException.class);
        verifyNoInteractions(addLibraryBooksUseCase);
    }

}
