package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@Transactional
class PersonalizedRecommendationConsentIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UpdatePersonalizedRecommendationConsentUseCase updateConsentUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void V21은_활성_개인화_추천_선택_약관을_하나_시드한다() {
        final Map<String, Object> term = jdbc.queryForMap("SELECT title, version, is_required + 0 AS required, display_order FROM terms "
            + "WHERE term_type = 'PERSONALIZED_RECOMMENDATION' AND is_active = b'1'");

        assertThat(term.get("title")).isEqualTo("개인화 도서 추천을 위한 정보 수집·이용 동의");
        assertThat(term.get("version")).isEqualTo("1.0");
        assertThat(((Number) term.get("required")).intValue()).isZero();
        assertThat(term.get("display_order")).isEqualTo(1);
    }

    @Test
    void 미동의_요청은_성공하고_동의_이력을_만들지_않는다() {
        final Long userId = insertUser("시드동의회원1");

        final var result = updateConsentUseCase.execute(new UpdatePersonalizedRecommendationConsentCommand(userId, false));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
        assertThat(agreementCount(userId)).isZero();
    }

    @Test
    void 동의하면_AGREE_이력이_한_건_생기고_다시_동의해도_늘지_않는다() {
        final Long userId = insertUser("시드동의회원2");

        final var first = updateConsentUseCase.execute(new UpdatePersonalizedRecommendationConsentCommand(userId, true));
        final var second = updateConsentUseCase.execute(new UpdatePersonalizedRecommendationConsentCommand(userId, true));

        assertThat(first.consented()).isTrue();
        assertThat(second.agreedAt()).isEqualTo(first.agreedAt());
        assertThat(agreementCount(userId)).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_동의는_섞이지_않는다() {
        final Long agreedUserId = insertUser("시드동의회원3");
        final Long otherUserId = insertUser("시드동의회원4");
        updateConsentUseCase.execute(new UpdatePersonalizedRecommendationConsentCommand(agreedUserId, true));

        final var other = updateConsentUseCase.execute(new UpdatePersonalizedRecommendationConsentCommand(otherUserId, false));

        assertThat(other.consented()).isFalse();
        assertThat(agreementCount(otherUserId)).isZero();
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private int agreementCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreement WHERE user_id = ?", Integer.class, userId);
    }
}
