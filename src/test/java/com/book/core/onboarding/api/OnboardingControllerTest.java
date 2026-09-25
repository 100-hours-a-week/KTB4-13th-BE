package com.book.core.onboarding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.onboarding.api.converter.OnboardingCommandConverter;
import com.book.core.onboarding.api.converter.OnboardingResultConverter;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.service.OnboardingService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    OnboardingCommandConverter.class,
    OnboardingResultConverter.class,
    OnboardingControllerTest.TestWebMvcConfig.class
})
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
        when(onboardingService.getQuestion(command))
                .thenReturn(new OnboardingQuestionResult(
                        1L,
                        "reading-time",
                        "주로 언제 책을 읽으시나요?",
                        1,
                        5,
                        List.of(new OnboardingOptionResult(1L, "morning", "아침, 하루를 시작할 때")),
                        2L));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionId").value(1))
                .andExpect(jsonPath("$.data.code").doesNotExist())
                .andExpect(jsonPath("$.data.content").value("주로 언제 책을 읽으시나요?"))
                .andExpect(jsonPath("$.data.minSelection").value(1))
                .andExpect(jsonPath("$.data.maxSelection").value(5))
                .andExpect(jsonPath("$.data.options[0].optionId").value(1))
                .andExpect(jsonPath("$.data.options[0].code").value("morning"))
                .andExpect(jsonPath("$.data.nextQuestionId").value(2));

        verify(onboardingService).getQuestion(command);
    }

    @Test
    void questionId가_0_이하이면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
    }

    @Test
    void 존재하지_않는_질문이면_404를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getQuestion(new GetOnboardingQuestionCommand(USER_ID, 999L)))
                .thenThrow(new com.book.common.exception.CoreException(
                        com.book.common.exception.ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 선행_질문_미응답이면_409를_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(onboardingService.getQuestion(new GetOnboardingQuestionCommand(USER_ID, 4L)))
                .thenThrow(new com.book.common.exception.CoreException(
                        com.book.common.exception.ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED));

        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E409"));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        final AbstractAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static class TestWebMvcConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
