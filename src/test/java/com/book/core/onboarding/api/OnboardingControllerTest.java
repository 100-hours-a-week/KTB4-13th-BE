package com.book.core.onboarding.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.api.converter.OnboardingCommandConverter;
import com.book.core.onboarding.api.converter.OnboardingResultConverter;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.service.OnboardingService;
import com.book.core.onboarding.domain.OnboardingStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OnboardingCommandConverter.class, OnboardingResultConverter.class, UserIdMvcConfig.class})
class OnboardingControllerTest {
    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OnboardingService onboardingService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 질문을_조회하고_응답_계약을_반환한다() throws Exception {
        authenticateAs(USER_ID);
        final GetOnboardingQuestionCommand command = new GetOnboardingQuestionCommand(USER_ID, 1L);
        when(onboardingService.getQuestion(command)).thenReturn(new OnboardingQuestionResult(1L, "reading-time", "주로 언제 책을 읽으시나요?", 1, 5,
            List.of(new OnboardingOptionResult(1L, null, "morning", "아침, 하루를 시작할 때")), 2L));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 1L)).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.questionId").value(1))
            .andExpect(jsonPath("$.data.code").doesNotExist()).andExpect(jsonPath("$.data.content").value("주로 언제 책을 읽으시나요?"))
            .andExpect(jsonPath("$.data.minSelection").value(1)).andExpect(jsonPath("$.data.maxSelection").value(5))
            .andExpect(jsonPath("$.data.options[0].optionId").value(1)).andExpect(jsonPath("$.data.options[0].code").value("morning"))
            .andExpect(jsonPath("$.data.options[0].parentOptionId").value(nullValue()))
            .andExpect(jsonPath("$.data.nextQuestionId").value(2));

        verify(onboardingService).getQuestion(command);
    }

    @Test
    void Q4_option은_parentOptionId를_응답에_포함한다() throws Exception {
        authenticateAs(USER_ID);
        final GetOnboardingQuestionCommand command = new GetOnboardingQuestionCommand(USER_ID, 4L);
        when(onboardingService.getQuestion(command)).thenReturn(new OnboardingQuestionResult(4L, "sub-genre", "세부 장르를 선택해주세요", 1, 3,
            List.of(new OnboardingOptionResult(23L, 9L, "novel-thriller", "추리/스릴러")), null));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[0].optionId").value(23)).andExpect(jsonPath("$.data.options[0].parentOptionId").value(9))
            .andExpect(jsonPath("$.data.options[0].code").value("novel-thriller"));

        verify(onboardingService).getQuestion(command);
    }

    @Test
    void questionId가_0_이하이면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 0L)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
    }

    @Test
    void 존재하지_않는_질문이면_404를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getQuestion(new GetOnboardingQuestionCommand(USER_ID, 999L)))
            .thenThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 999L)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 선행_질문_미응답이면_409를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getQuestion(new GetOnboardingQuestionCommand(USER_ID, 4L))).thenThrow(
            new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L)).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("E409"));
    }

    @Test
    void 부모_선택지_ID를_전달하면_Command에_담아_Q4_선택지를_조회한다() throws Exception {
        authenticateAs(USER_ID);
        final GetOnboardingQuestionCommand command = new GetOnboardingQuestionCommand(USER_ID, 4L, List.of(9L, 10L));
        when(onboardingService.getQuestion(command)).thenReturn(new OnboardingQuestionResult(4L, "subcategory", "세부 카테고리", 1, 9,
            List.of(new OnboardingOptionResult(23L, 9L, "novel-thriller", "추리/스릴러"),
                new OnboardingOptionResult(28L, 10L, "humanities-psychology", "심리학")),
            null));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L).param("parentOptionIds", "9,10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[0].optionId").value(23)).andExpect(jsonPath("$.data.options[1].parentOptionId").value(10));
        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L).param("parentOptionIds", "9", "10")).andExpect(status().isOk());

        verify(onboardingService, times(2)).getQuestion(command);
    }

    @Test
    void 부모_선택지_ID가_빈_값이거나_유효하지_않으면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        for (final String parentOptionIds : List.of("", "0", "-1", "9,9", "abc")) {
            mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L).param("parentOptionIds", parentOptionIds))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        }
        verifyNoInteractions(onboardingService);
    }

    @Test
    void 진행_정보를_조회하고_응답_계약을_반환한다() throws Exception {
        authenticateAs(USER_ID);
        final GetOnboardingProgressCommand command = new GetOnboardingProgressCommand(USER_ID);
        when(onboardingService.getProgress(command)).thenReturn(new OnboardingProgressResult(OnboardingStatus.COMPLETED,
            LocalDateTime.of(2026, 1, 1, 0, 0), List.of(new OnboardingAnswerGroupResult(1L, List.of(1L, 2L))), List.of(10L, 20L)));

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("COMPLETED")).andExpect(jsonPath("$.data.answers[0].questionId").value(1))
            .andExpect(jsonPath("$.data.answers[0].optionIds[0]").value(1)).andExpect(jsonPath("$.data.answers[0].optionIds[1]").value(2))
            .andExpect(jsonPath("$.data.bookIds[0]").value(10)).andExpect(jsonPath("$.data.bookIds[1]").value(20));

        verify(onboardingService).getProgress(command);
    }

    @Test
    void 진행중인_온보딩이_없으면_404를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getProgress(new GetOnboardingProgressCommand(USER_ID)))
            .thenThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_NOT_FOUND));

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 답변을_저장하고_200을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        final PutOnboardingAnswersCommand command = new PutOnboardingAnswersCommand(USER_ID, 1L, List.of(1L, 2L));

        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", 1L).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":[1,2]}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(onboardingService).saveAnswers(command);
    }

    @Test
    void 선택지_개수가_유효하지_않으면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        doThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION))
            .when(onboardingService).saveAnswers(new PutOnboardingAnswersCommand(USER_ID, 1L, List.of()));

        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", 1L).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":[]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
    }

    @Test
    void 선행_질문_응답이_없으면_답변_저장시_409를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        doThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED))
            .when(onboardingService).saveAnswers(new PutOnboardingAnswersCommand(USER_ID, 4L, List.of(23L)));

        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", 4L).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":[23]}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"));
    }

    @Test
    void 도서_선택을_저장하고_200을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        final PutOnboardingBooksCommand command = new PutOnboardingBooksCommand(USER_ID, List.of(10L, 20L));

        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[10,20]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(onboardingService).saveBooks(command);
    }

    @Test
    void 존재하지_않는_도서가_포함되면_404를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        doThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_BOOK_NOT_FOUND))
            .when(onboardingService).saveBooks(new PutOnboardingBooksCommand(USER_ID, List.of(999L)));

        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[999]}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 진행중인_온보딩이_없으면_도서_저장시_404를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        doThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.ONBOARDING_NOT_FOUND))
            .when(onboardingService).saveBooks(new PutOnboardingBooksCommand(USER_ID, List.of()));

        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 도서_후보를_조회하고_응답_계약을_반환한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getBookCandidates(new GetOnboardingBookCandidatesCommand(List.of("novel-sf", "science-space"))))
            .thenReturn(List.of(new OnboardingBookCandidateResult(10L, "제목1", "작가1", "https://example.com/1.jpg"),
                new OnboardingBookCandidateResult(20L, "제목2", "작가2", "https://example.com/2.jpg")));

        mvc.perform(get("/api/v1/onboarding/books").param("subcategoryCodes", "novel-sf", "science-space")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.candidates[0].bookId").value(10))
            .andExpect(jsonPath("$.data.candidates[0].title").value("제목1")).andExpect(jsonPath("$.data.candidates[1].bookId").value(20));

        verify(onboardingService).getBookCandidates(new GetOnboardingBookCandidatesCommand(List.of("novel-sf", "science-space")));
    }

    @Test
    void 도서_후보가_없으면_빈_배열을_반환한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getBookCandidates(new GetOnboardingBookCandidatesCommand(List.of("novel-sf", "science-space"))))
            .thenReturn(List.of());

        mvc.perform(get("/api/v1/onboarding/books").param("subcategoryCodes", "novel-sf", "science-space")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.candidates").isArray()).andExpect(jsonPath("$.data.candidates").isEmpty());
    }

    @Test
    void 세부_카테고리가_누락되거나_빈_값이면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        mvc.perform(get("/api/v1/onboarding/books")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/onboarding/books").param("subcategoryCodes", "")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
    }

    @Test
    void 쉼표로_전달한_세부_카테고리는_중복_제거한_Command로_전달한다() throws Exception {
        authenticateAs(USER_ID);
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf", "science-space"));
        when(onboardingService.getBookCandidates(command)).thenReturn(List.of());
        mvc.perform(get("/api/v1/onboarding/books").param("subcategoryCodes", "novel-sf,science-space,novel-sf"))
            .andExpect(status().isOk());
        verify(onboardingService).getBookCandidates(command);
    }

    @Test
    void 본문_없는_개인화_동의_요청을_JWT_사용자의_커맨드로_전달한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(post("/api/v1/onboarding/personalization-agreement")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").doesNotExist());

        verify(onboardingService).recordPersonalizationAgreement(new RecordPersonalizationAgreementCommand(USER_ID));
    }

    @Test
    void 취향_정보_전체_수정_요청을_Command로_전달하고_200을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content("""
            {"answers":[{"questionId":1,"optionIds":[1,2]},{"questionId":2,"optionIds":[6]},
            {"questionId":3,"optionIds":[9]},{"questionId":4,"optionIds":[23]}],"bookIds":[10,20]}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").doesNotExist());

        verify(onboardingService).updatePreferences(new PutOnboardingPreferencesCommand(USER_ID,
            List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L, 2L)), new OnboardingPreferenceAnswerCommand(2L, List.of(6L)),
                new OnboardingPreferenceAnswerCommand(3L, List.of(9L)), new OnboardingPreferenceAnswerCommand(4L, List.of(23L))),
            List.of(10L, 20L)));
    }

    @Test
    void 취향_정보_전체_수정_요청_형식이_잘못되면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        for (final String body : List.of("{\"bookIds\":[]}", "{\"answers\":[]}", "{\"answers\":[null],\"bookIds\":[]}",
            "{\"answers\":[{\"questionId\":0,\"optionIds\":[1]}],\"bookIds\":[]}",
            "{\"answers\":[{\"questionId\":1,\"optionIds\":[null]}],\"bookIds\":[]}",
            "{\"answers\":[{\"questionId\":1,\"optionIds\":[1]}],\"bookIds\":[0]}",
            "{\"answers\":[{\"questionId\":1,\"optionIds\":[1]},{\"questionId\":1,\"optionIds\":[2]}],\"bookIds\":[]}")) {
            mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        }
        verifyNoInteractions(onboardingService);
    }

    @Test
    void 취향_정보_전체_수정의_신규_오류_코드를_HTTP_응답으로_변환한다() throws Exception {
        authenticateAs(USER_ID);

        expectUpdatePreferencesError(ErrorCode.ONBOARDING_NOT_COMPLETED, 409, "E409");
        expectUpdatePreferencesError(ErrorCode.INVALID_ONBOARDING_PREFERENCES, 400, "E400");
    }

    private void expectUpdatePreferencesError(final ErrorCode errorCode, final int status, final String code) throws Exception {
        doThrow(new CoreException(errorCode)).when(onboardingService).updatePreferences(any(PutOnboardingPreferencesCommand.class));

        mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"answers\":[{\"questionId\":1,\"optionIds\":[1]}],\"bookIds\":[]}")).andExpect(status().is(status))
            .andExpect(jsonPath("$.code").value(code)).andExpect(jsonPath("$.message").value(errorCode.message()));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        final AbstractAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
