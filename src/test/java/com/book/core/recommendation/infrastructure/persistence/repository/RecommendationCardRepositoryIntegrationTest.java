package com.book.core.recommendation.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
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
class RecommendationCardRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    RecommendationCardRepositoryPort recommendationCardRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 카드를_저장하면_소유_사용자와_도서로_영속된다() {
        insertUser(9001L, "닉네임9001");
        insertBook(9101L, "저장 검증 도서");

        final var saved = recommendationCardRepository.save(RecommendationCard.create(9001L, 9101L, "긴 추천 이유"));

        final var row = jdbc.queryForMap("SELECT user_id, book_id, reason_long FROM recommendation_cards WHERE id = ?", saved.id());
        assertThat(row).containsEntry("user_id", 9001L).containsEntry("book_id", 9101L).containsEntry("reason_long", "긴 추천 이유");
    }

    @Test
    void 카드_ID로_조회하면_저장된_카드를_찾는다() {
        insertUser(9002L, "닉네임9002");
        insertBook(9102L, "조회 검증 도서");
        final var saved = recommendationCardRepository.save(RecommendationCard.create(9002L, 9102L, "조회용 이유"));

        final var found = recommendationCardRepository.findById(saved.id()).orElseThrow();

        assertThat(found.userId()).isEqualTo(9002L);
        assertThat(found.bookId()).isEqualTo(9102L);
        assertThat(found.isOwnedBy(9002L)).isTrue();
        assertThat(found.isOwnedBy(9999L)).isFalse();
    }

    @Test
    void 다른_사용자가_소유한_카드는_isOwnedBy로_거부된다() {
        insertUser(9003L, "닉네임9003");
        insertUser(9004L, "닉네임9004");
        insertBook(9103L, "소유권 검증 도서");
        final var saved = recommendationCardRepository.save(RecommendationCard.create(9003L, 9103L, "소유권 이유"));

        final var found = recommendationCardRepository.findById(saved.id()).orElseThrow();

        assertThat(found.isOwnedBy(9004L)).isFalse();
    }

    @Test
    void 존재하지_않는_카드는_빈_결과를_반환한다() {
        assertThat(recommendationCardRepository.findById(999_999L)).isEmpty();
    }

    private void insertUser(final long id, final String nickname) {
        jdbc.update("""
            INSERT INTO users (id, nickname, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, nickname);
    }

    private void insertBook(final long id, final String title) {
        jdbc.update("""
            INSERT INTO books (
                id, title, author, publisher, category, published_at, deleted_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, title, "작가", "출판사", "소설", "2026-01-01", (Timestamp) null);
    }
}
