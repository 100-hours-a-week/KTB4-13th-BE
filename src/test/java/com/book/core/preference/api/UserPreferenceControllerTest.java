package com.book.core.preference.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.memory.domain.UserMemoryType;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.preference.api.converter.UserPreferenceCommandConverter;
import com.book.core.preference.api.converter.UserPreferenceResultConverter;
import com.book.core.preference.application.result.UserPreferencesResult;
import com.book.core.preference.application.service.UserPreferenceService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({UserPreferenceCommandConverter.class, UserPreferenceResultConverter.class, UserIdMvcConfig.class})
class UserPreferenceControllerTest {
    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserPreferenceService userPreferenceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 온보딩_선택지_이름과_묶은_취향_기억을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(userPreferenceService.getPreferences(USER_ID)).thenReturn(new UserPreferencesResult(
            List.of(new OnboardingAnswerGroupResult(3L, List.of(new OnboardingOptionResult(9L, null, "economy", "경제경영")))),
            List.of(new UserMemoryGroupResult(List.of(102L, 101L), UserMemoryType.TOPIC, "경제 책을 좋아해요", List.of("경제경영"),
                List.of(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 12))))));

        mvc.perform(get("/api/v1/users/me/preferences")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.onboarding[0].questionId").value(3))
            .andExpect(jsonPath("$.data.onboarding[0].options[0].optionId").value(9))
            .andExpect(jsonPath("$.data.onboarding[0].options[0].name").value("경제경영"))
            .andExpect(jsonPath("$.data.onboarding[0].options[0].code").doesNotExist())
            .andExpect(jsonPath("$.data.memories[0].memoryIds[0]").value(102))
            .andExpect(jsonPath("$.data.memories[0].memoryIds[1]").value(101)).andExpect(jsonPath("$.data.memories[0].type").value("topic"))
            .andExpect(jsonPath("$.data.memories[0].value").value("경제 책을 좋아해요"))
            .andExpect(jsonPath("$.data.memories[0].categories[0]").value("경제경영"))
            .andExpect(jsonPath("$.data.memories[0].conversationDates[0]").value("2026-10-16"))
            .andExpect(jsonPath("$.data.memories[0].conversationDates[1]").value("2026-10-12"));
    }

    @Test
    void 취향_기억이_없으면_빈_배열을_응답한다() throws Exception {
        authenticateAs(USER_ID);
        when(userPreferenceService.getPreferences(USER_ID)).thenReturn(new UserPreferencesResult(List.of(), List.of()));

        mvc.perform(get("/api/v1/users/me/preferences")).andExpect(status().isOk()).andExpect(jsonPath("$.data.onboarding").isEmpty())
            .andExpect(jsonPath("$.data.memories").isArray()).andExpect(jsonPath("$.data.memories").isEmpty());
    }

    @Test
    void 선택한_취향_기억을_중복_없이_삭제하고_204를_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(delete("/api/v1/users/me/memories").param("ids", "102,101,102")).andExpect(status().isNoContent());

        verify(userPreferenceService).deleteMemories(new DeleteUserMemoriesCommand(USER_ID, List.of(102L, 101L)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "101,abc"})
    void 삭제할_ID가_비었거나_양수가_아니면_400을_응답한다(final String ids) throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(delete("/api/v1/users/me/memories").param("ids", ids)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(userPreferenceService);
    }

    @Test
    void 삭제할_ID가_없으면_400을_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(delete("/api/v1/users/me/memories")).andExpect(status().isBadRequest());

        verifyNoInteractions(userPreferenceService);
    }

    @Test
    void 취향_기억을_전체_삭제하고_204를_응답한다() throws Exception {
        authenticateAs(USER_ID);

        mvc.perform(delete("/api/v1/users/me/memories/all")).andExpect(status().isNoContent());

        verify(userPreferenceService).deleteAllMemories(USER_ID);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
