package com.book.core.onboarding.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("integration")
class OnboardingQuestionParentOptionApiIntegrationTest {
    private static final String QUESTION_URL = "/api/v1/onboarding/questions/{questionId}";
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 부모_선택지를_생략하면_저장된_Q3_답변_기준을_유지하고_전달하면_저장된_답변_대신_전달한_Q3_기준으로_Q4를_조회한다() throws Exception {
        authenticateAs(insertUser("Q4필터회원1"));
        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", 3).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":[9]}")).andExpect(status().isOk());

        mvc.perform(get(QUESTION_URL, 4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[*].optionId", contains(23, 24, 25, 26, 27)))
            .andExpect(jsonPath("$.data.options[*].parentOptionId", everyItem(is(9))));
        mvc.perform(get(QUESTION_URL, 4).param("parentOptionIds", "10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[*].optionId", contains(28, 29, 30, 31)))
            .andExpect(jsonPath("$.data.options[*].parentOptionId", everyItem(is(10))));
    }

    @Test
    void 저장된_Q3_답변이_없어도_부모_선택지를_전달하면_Q4를_조회하고_생략하면_기존처럼_409를_응답한다() throws Exception {
        authenticateAs(insertUser("Q4필터회원2"));

        mvc.perform(get(QUESTION_URL, 4)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"));
        mvc.perform(get(QUESTION_URL, 4).param("parentOptionIds", "9", "10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.options[*].parentOptionId", everyItem(is(oneOf(9, 10)))));
    }

    @Test
    void 유효하지_않은_부모_선택지는_실제_선택지_데이터_기준으로_거부한다() throws Exception {
        authenticateAs(insertUser("Q4필터회원3"));

        mvc.perform(get(QUESTION_URL, 1).param("parentOptionIds", "9")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get(QUESTION_URL, 4).param("parentOptionIds", "999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("E404"));
        mvc.perform(get(QUESTION_URL, 4).param("parentOptionIds", "23")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("E409"));
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
