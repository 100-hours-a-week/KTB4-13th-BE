package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OnboardingProfileFailureIsolationIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OnboardingService onboardingService;

    @MockitoBean
    TermRepositoryPort termRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 온보딩_완료가_커밋된_뒤_AI_취향_프로필_생성이_실패해도_완료_요청은_성공한다() {
        final Long userId = insertUser("프로필실패격리회원");
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        when(termRepository.findActiveByTermType(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatCode(() -> onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of()))).doesNotThrowAnyException();

        assertThat(jdbc.queryForObject("SELECT onboarding_status FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL",
            String.class, userId)).isEqualTo("COMPLETED");
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }
}
