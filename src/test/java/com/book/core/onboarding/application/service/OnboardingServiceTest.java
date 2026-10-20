package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.CannotCreateTransactionException;

class OnboardingServiceTest {
    private static final Long USER_ID = 42L;
    private static final PutOnboardingBooksCommand COMMAND = new PutOnboardingBooksCommand(USER_ID, List.of(10L));
    private static final PutOnboardingPreferencesCommand PREFERENCES_COMMAND =
        new PutOnboardingPreferencesCommand(USER_ID, List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L))), List.of(10L));
    private static final AddLibraryBooksCommand LIBRARY_COMMAND = new AddLibraryBooksCommand(USER_ID, List.of(10L));

    private final SaveOnboardingBooksUseCase saveOnboardingBooksUseCase = mock(SaveOnboardingBooksUseCase.class);
    private final PersonalizationProfileUseCase personalizationProfileUseCase = mock(PersonalizationProfileUseCase.class);
    private final GetOnboardingBookCandidatesUseCase getBookCandidatesUseCase = mock(GetOnboardingBookCandidatesUseCase.class);
    private final UpdateOnboardingPreferencesUseCase updateOnboardingPreferencesUseCase = mock(UpdateOnboardingPreferencesUseCase.class);
    private final AddLibraryBooksUseCase addLibraryBooksUseCase = mock(AddLibraryBooksUseCase.class);
    private final OnboardingService onboardingService = new OnboardingService(mock(GetOnboardingQuestionUseCase.class),
        mock(GetOnboardingProgressUseCase.class), mock(SaveOnboardingAnswersUseCase.class), saveOnboardingBooksUseCase,
        getBookCandidatesUseCase, mock(RecordPersonalizationAgreementUseCase.class), personalizationProfileUseCase,
        updateOnboardingPreferencesUseCase, addLibraryBooksUseCase);

    @BeforeEach
    void completeOnboardingForTheFirstTime() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(true));
    }

    @Test
    void 후보_조회_Command를_UseCase로_전달한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        onboardingService.getBookCandidates(command);
        verify(getBookCandidatesUseCase).execute(command);
        verifyNoInteractions(saveOnboardingBooksUseCase, personalizationProfileUseCase);
    }

    @Test
    void 취향_정보가_변경되면_수정이_끝난_뒤_AI_취향_프로필_갱신을_요청한다() {
        when(updateOnboardingPreferencesUseCase.execute(PREFERENCES_COMMAND)).thenReturn(true);

        onboardingService.updatePreferences(PREFERENCES_COMMAND);

        final var ordered = inOrder(updateOnboardingPreferencesUseCase, personalizationProfileUseCase);
        ordered.verify(updateOnboardingPreferencesUseCase).execute(PREFERENCES_COMMAND);
        ordered.verify(personalizationProfileUseCase).executeUpdate(USER_ID);
        verify(personalizationProfileUseCase, never()).execute(USER_ID);
        verifyNoInteractions(saveOnboardingBooksUseCase);
    }

    @Test
    void 취향_정보_변경이_없으면_AI를_호출하지_않는다() {
        when(updateOnboardingPreferencesUseCase.execute(PREFERENCES_COMMAND)).thenReturn(false);

        onboardingService.updatePreferences(PREFERENCES_COMMAND);

        verify(updateOnboardingPreferencesUseCase).execute(PREFERENCES_COMMAND);
        verifyNoInteractions(personalizationProfileUseCase);
    }

    @Test
    void 취향_정보_수정이_실패하면_AI를_호출하지_않는다() {
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_COMPLETED)).when(updateOnboardingPreferencesUseCase)
            .execute(PREFERENCES_COMMAND);

        assertThatThrownBy(() -> onboardingService.updatePreferences(PREFERENCES_COMMAND)).isInstanceOf(CoreException.class);
        verifyNoInteractions(personalizationProfileUseCase);
    }

    @Test
    void AI_취향_프로필_갱신이_실패해도_취향_정보_수정은_성공한다() {
        when(updateOnboardingPreferencesUseCase.execute(PREFERENCES_COMMAND)).thenReturn(true);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(personalizationProfileUseCase).executeUpdate(USER_ID);

        assertThatCode(() -> onboardingService.updatePreferences(PREFERENCES_COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void 최초_완료하면_온보딩_저장_도서관_추가_AI_취향_프로필_생성_순서로_실행한다() {
        onboardingService.saveBooks(COMMAND);

        final var ordered = inOrder(saveOnboardingBooksUseCase, addLibraryBooksUseCase, personalizationProfileUseCase);
        ordered.verify(saveOnboardingBooksUseCase).execute(COMMAND);
        ordered.verify(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);
        ordered.verify(personalizationProfileUseCase).execute(USER_ID);
        verify(personalizationProfileUseCase, never()).executeUpdate(USER_ID);
    }

    @Test
    void 이미_완료한_온보딩의_재요청은_도서관에_추가하지_않고_AI_취향_프로필_생성만_요청한다() {
        when(saveOnboardingBooksUseCase.execute(COMMAND)).thenReturn(new SaveOnboardingBooksResult(false));

        onboardingService.saveBooks(COMMAND);

        verifyNoInteractions(addLibraryBooksUseCase);
        verify(personalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 도서관_추가의_일시적인_DB_오류는_최대_3회_시도한_뒤_격리하고_AI_취향_프로필_생성을_요청한다() {
        doThrow(new CannotAcquireLockException("deadlock")).when(addLibraryBooksUseCase).execute(LIBRARY_COMMAND);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
        verify(addLibraryBooksUseCase, times(3)).execute(LIBRARY_COMMAND);
        verify(personalizationProfileUseCase).execute(USER_ID);
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
        verify(personalizationProfileUseCase).execute(USER_ID);
    }

    @Test
    void 온보딩_도서_저장이_실패하면_AI를_호출하지_않는다() {
        doThrow(new CoreException(ErrorCode.ONBOARDING_NOT_FOUND)).when(saveOnboardingBooksUseCase).execute(COMMAND);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(CoreException.class);
        verifyNoInteractions(addLibraryBooksUseCase, personalizationProfileUseCase);
    }

    @Test
    void AI_취향_프로필_생성이_실패해도_온보딩_완료는_성공한다() {
        doThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE)).when(personalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_응답_원문이_예외에_포함되어도_로그에는_기록하지_않는다() {
        final Logger logger = (Logger) LoggerFactory.getLogger(OnboardingService.class);
        final Level previousLevel = logger.getLevel();
        final boolean previousAdditive = logger.isAdditive();
        final Map<String, String> previousMdc = org.slf4j.MDC.getCopyOfContextMap();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        logger.addAppender(appender);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE, new RestClientException("private-external-response-body")))
            .when(personalizationProfileUseCase).execute(USER_ID);

        try {
            onboardingService.saveBooks(COMMAND);

            assertThat(appender.list).singleElement().satisfies((final ILoggingEvent event) -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("[ONBOARDING]", "errorCode=AI_SERVICE_FAILURE")
                    .doesNotContain("private-external-response-body");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            appender.stop();
            org.slf4j.MDC.clear();
            if (previousMdc != null) {
                org.slf4j.MDC.setContextMap(previousMdc);
            }
        }
    }

    @Test
    void AI_취향_프로필_생성_중_DB_조회가_실패해도_온보딩_완료는_성공한다() {
        doThrow(new DataAccessResourceFailureException("db down")).when(personalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_취향_프로필_생성_중_DB_연결을_얻지_못해도_온보딩_완료는_성공한다() {
        doThrow(new CannotCreateTransactionException("no connection")).when(personalizationProfileUseCase).execute(USER_ID);

        assertThatCode(() -> onboardingService.saveBooks(COMMAND)).doesNotThrowAnyException();
    }

    @Test
    void AI_실패가_아닌_예외는_숨기지_않는다() {
        doThrow(new IllegalStateException("bug")).when(personalizationProfileUseCase).execute(USER_ID);

        assertThatThrownBy(() -> onboardingService.saveBooks(COMMAND)).isInstanceOf(IllegalStateException.class);
    }
}
