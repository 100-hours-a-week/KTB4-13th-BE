package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.RecordPersonalizedRecommendationConsentCommand;
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
    RecordPersonalizedRecommendationConsentUseCase recordConsentUseCase;

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
    void 동의하면_AGREE_이력이_한_건_생기고_다시_동의해도_늘지_않는다() {
        final Long userId = insertUser("시드동의회원2");

        final var first = recordConsentUseCase.execute(new RecordPersonalizedRecommendationConsentCommand(userId));
        final var second = recordConsentUseCase.execute(new RecordPersonalizedRecommendationConsentCommand(userId));

        assertThat(first.consented()).isTrue();
        assertThat(second.agreedAt()).isEqualTo(first.agreedAt());
        assertThat(agreementCount(userId)).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_동의는_각자의_이력으로_기록된다() {
        final Long userId = insertUser("시드동의회원3");
        final Long otherUserId = insertUser("시드동의회원4");

        recordConsentUseCase.execute(new RecordPersonalizedRecommendationConsentCommand(userId));
        recordConsentUseCase.execute(new RecordPersonalizedRecommendationConsentCommand(otherUserId));

        assertThat(agreementCount(userId)).isEqualTo(1);
        assertThat(agreementCount(otherUserId)).isEqualTo(1);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private int agreementCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreement WHERE user_id = ?", Integer.class, userId);
    }
}
