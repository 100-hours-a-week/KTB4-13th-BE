package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SaveOnboardingAnswersIntegrationTest {
    private static final long MAIN_CATEGORY_QUESTION_ID = 3L;
    private static final long SUBCATEGORY_QUESTION_ID = 4L;
    private static final long NOVEL_OPTION_ID = 9L;
    private static final long NOVEL_THRILLER_OPTION_ID = 23L;
    private static final long NOVEL_SF_OPTION_ID = 24L;

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    SaveOnboardingAnswersUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 기존_선택을_포함한_새_답변으로_교체해도_UNIQUE_충돌_없이_저장된다() {
        final Long userId = userWithSubcategoryAnswer("답변교체회원1", List.of(NOVEL_THRILLER_OPTION_ID));

        useCase.execute(
            new PutOnboardingAnswersCommand(userId, SUBCATEGORY_QUESTION_ID, List.of(NOVEL_THRILLER_OPTION_ID, NOVEL_SF_OPTION_ID)));

        assertThat(subcategoryOptionIds(userId)).containsExactlyInAnyOrder(NOVEL_THRILLER_OPTION_ID, NOVEL_SF_OPTION_ID);
    }

    @Test
    void 기존_선택_일부를_제거한_답변으로_교체해도_저장된다() {
        final Long userId = userWithSubcategoryAnswer("답변교체회원2", List.of(NOVEL_THRILLER_OPTION_ID, NOVEL_SF_OPTION_ID));

        useCase.execute(new PutOnboardingAnswersCommand(userId, SUBCATEGORY_QUESTION_ID, List.of(NOVEL_SF_OPTION_ID)));

        assertThat(subcategoryOptionIds(userId)).containsExactly(NOVEL_SF_OPTION_ID);
    }

    private Long userWithSubcategoryAnswer(final String nickname, final List<Long> subcategoryOptionIds) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        final Long userId = jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
        useCase.execute(new PutOnboardingAnswersCommand(userId, MAIN_CATEGORY_QUESTION_ID, List.of(NOVEL_OPTION_ID)));
        useCase.execute(new PutOnboardingAnswersCommand(userId, SUBCATEGORY_QUESTION_ID, subcategoryOptionIds));
        return userId;
    }

    private List<Long> subcategoryOptionIds(final Long userId) {
        return jdbc.queryForList("""
            SELECT answer.onboarding_option_id
            FROM user_onboarding_answers answer
            JOIN onboarding_options onboarding_option ON onboarding_option.id = answer.onboarding_option_id
            WHERE answer.user_id = ? AND onboarding_option.onboarding_questions_id = ?
            """, Long.class, userId, SUBCATEGORY_QUESTION_ID);
    }
}
