package com.book.core.onboarding.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("integration")
class OnboardingPreferencesApiIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 최초_온보딩을_완료한_뒤_Q3와_Q4를_함께_전체_수정한다() throws Exception {
        authenticateAs(insertUser("전체수정API회원1"));
        saveAnswers(1, "[1]");
        saveAnswers(2, "[6]");
        saveAnswers(3, "[9]");
        mvc.perform(get("/api/v1/onboarding/questions/{questionId}", 4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[*].parentOptionId", everyItem(is(9))));
        saveAnswers(4, "[23]");
        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[]}"))
            .andExpect(status().isOk());

        mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content("""
            {"answers":[{"questionId":1,"optionIds":[1]},{"questionId":2,"optionIds":[6]},
            {"questionId":3,"optionIds":[10]},{"questionId":4,"optionIds":[28]}],"bookIds":[]}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.answers[2].questionId").value(3)).andExpect(jsonPath("$.data.answers[2].optionIds", contains(10)))
            .andExpect(jsonPath("$.data.answers[3].questionId").value(4)).andExpect(jsonPath("$.data.answers[3].optionIds", contains(28)));
    }

    @Test
    void 완료되지_않은_온보딩을_전체_수정하면_409를_응답하고_답변을_유지한다() throws Exception {
        final Long userId = insertUser("전체수정API회원2");
        authenticateAs(userId);
        saveAnswers(1, "[1]");

        mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content("""
            {"answers":[{"questionId":1,"optionIds":[2]},{"questionId":2,"optionIds":[6]},
            {"questionId":3,"optionIds":[9]},{"questionId":4,"optionIds":[23]}],"bookIds":[]}
            """)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"))
            .andExpect(jsonPath("$.message").value("완료되지 않은 온보딩은 수정할 수 없습니다."));

        mvc.perform(get("/api/v1/onboarding")).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.data.answers[0].optionIds", contains(1)));
    }

    @Test
    void Q3를_부분_변경하면_유지된_부모의_Q4_답변만_남는다() throws Exception {
        final Long userId = insertUser("부모부분변경회원");
        authenticateAs(userId);
        saveAnswers(3, "[9,10]");
        saveAnswers(4, "[23,28]");

        saveAnswers(3, "[10,11]");

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answers[0].optionIds", contains(10, 11)))
            .andExpect(jsonPath("$.data.answers[1].questionId").value(4))
            .andExpect(jsonPath("$.data.answers[1].optionIds", contains(28)));
        assertThat(jdbc.queryForList("SELECT onboarding_option_id FROM user_onboarding_answers WHERE user_id = ?", Long.class, userId))
            .containsExactlyInAnyOrder(10L, 11L, 28L);
    }

    @Test
    void Q3를_모두_변경하면_기존_Q4_답변을_모두_삭제한다() throws Exception {
        final Long userId = insertUser("부모전체변경회원");
        authenticateAs(userId);
        saveAnswers(3, "[9]");
        saveAnswers(4, "[23]");

        saveAnswers(3, "[10]");

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answers[0].optionIds", contains(10)))
            .andExpect(jsonPath("$.data.answers[1]").doesNotExist());
        assertThat(jdbc.queryForList("SELECT onboarding_option_id FROM user_onboarding_answers WHERE user_id = ?", Long.class, userId))
            .containsExactly(10L);
    }

    @Test
    void 진행_조회에서_이미_저장된_불일치_Q4_답변을_제외한다() throws Exception {
        final Long userId = insertUser("기존불일치답변회원");
        authenticateAs(userId);
        saveAnswers(3, "[10]");
        saveAnswers(4, "[28]");
        jdbc.update("INSERT INTO user_onboarding_answers (user_id, onboarding_option_id) VALUES (?, ?)", userId, 23);

        mvc.perform(get("/api/v1/onboarding")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.answers[1].optionIds", contains(28)));
    }

    private void saveAnswers(final int questionId, final String optionIds) throws Exception {
        final MockHttpServletRequestBuilder request = put("/api/v1/onboarding/questions/{questionId}/answers", questionId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"optionIds\":" + optionIds + "}");
        mvc.perform(request).andExpect(status().isOk());
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
