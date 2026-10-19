package com.book.core.memory.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import com.book.core.memory.domain.UserMemoryType;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class UserMemoryRepositoryIntegrationTest {
    private static final String NICKNAME = "기억저장소회원";
    private static final int CHECK_CONSTRAINT_VIOLATED = 3819;

    @Autowired
    UserMemoryRepositoryPort userMemoryRepository;

    @Autowired
    JdbcTemplate jdbc;

    Long userId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", NICKNAME);
        userId = jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, NICKNAME);
    }

    @Test
    void 카테고리와_벡터를_JSON_배열로_저장하고_목록으로_복원한다() {
        userMemoryRepository.saveAll(List.of(new UserMemory(userId, UserMemoryType.TOPIC, "추리 소설을 좋아함", 2, List.of("추리/스릴러", "소설"),
            "추리 소설", List.of(0.054f, -0.0028f, 1.0f), 3, "cv_1")));
        jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ?", userId);

        final UserMemory found = userMemoryRepository.findDeletedByUserIdAndConversationId(userId, "cv_1").getFirst();

        assertThat(found.categories()).containsExactly("추리/스릴러", "소설");
        assertThat(found.vector()).containsExactly(0.054f, -0.0028f, 1.0f);
        assertThat(found.createdAt()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|',
        value = {"UNKNOWN | 2 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_type",
            "TOPIC   | 0 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_strength",
            "TOPIC   | 4 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_strength",
            "TOPIC   | 2 | '[\"a\",\"b\",\"c\",\"d\"]' | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_categories",
            "TOPIC   | 2 | '{}'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_categories",
            "TOPIC   | 2 | '[]'                      | ' '       | '[0.1]'      | 1 | ck_user_memories_keyword_not_blank",
            "TOPIC   | 2 | '[]'                      | 추리 소설 | '[0.1, 0.2]' | 1 | ck_user_memories_vector"})
    void DB_제약을_어기는_기억은_저장하지_않는다(final String type, final int strength, final String categories, final String keyword, final String vector,
        final int dim, final String constraint) {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO user_memories (user_id, type, value, strength, categories, keyword, vector, dim, conversation_id)
            VALUES (?, ?, '기억 문장', ?, ?, ?, ?, ?, 'cv_1')
            """, userId, type, strength, categories, keyword, vector, dim)).isInstanceOfSatisfying(UncategorizedSQLException.class,
            (final UncategorizedSQLException exception) -> {
                final SQLException cause = exception.getSQLException();
                assertThat(cause.getErrorCode()).isEqualTo(CHECK_CONSTRAINT_VIOLATED);
                assertThat(cause.getMessage()).contains(constraint);
            });
    }
}
