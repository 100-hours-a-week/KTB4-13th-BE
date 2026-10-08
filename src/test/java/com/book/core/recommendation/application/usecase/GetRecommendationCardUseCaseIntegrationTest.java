package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.domain.RecommendationCard;
import java.sql.Timestamp;
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
class GetRecommendationCardUseCaseIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    RecommendationCardRepositoryPort recommendationCardRepository;

    @Autowired
    GetRecommendationCardUseCase getRecommendationCardUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void MySQL에_저장한_카드의_ID로_상세_추천_이유를_다시_조회한다() {
        insertUser(9005L, "닉네임9005");
        insertBook(9105L, "상세 재조회 검증 도서");
        final var saved = recommendationCardRepository.save(RecommendationCard.create(9005L, 9105L, "저장된 상세 추천 이유"));

        final var detail = getRecommendationCardUseCase.execute(new GetRecommendationCardCommand(9005L, saved.id()));

        assertThat(detail).isEqualTo(new RecommendationCardDetailResult(saved.id(), 9105L, "저장된 상세 추천 이유"));
    }

    private void insertUser(final long id, final String nickname) {
        jdbc.update(
            "INSERT INTO users (id, nickname, created_at, updated_at) " + "VALUES (?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", id,
            nickname);
    }

    private void insertBook(final long id, final String title) {
        jdbc.update(
            "INSERT INTO books (id, title, author, publisher, category, published_at, deleted_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))",
            id, title, "작가", "출판사", "소설", "2026-01-01", (Timestamp) null);
    }
}
