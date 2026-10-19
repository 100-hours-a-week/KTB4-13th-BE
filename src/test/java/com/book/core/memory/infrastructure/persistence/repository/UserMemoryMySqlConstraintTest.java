package com.book.core.memory.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.flywaydb.core.Flyway;

@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class UserMemoryMySqlConstraintTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @ParameterizedTest
    @CsvSource(delimiter = '|',
        value = {"UNKNOWN | 2 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_type",
            "TOPIC   | 0 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_strength",
            "TOPIC   | 4 | '[]'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_strength",
            "TOPIC   | 2 | '[\"a\",\"b\",\"c\",\"d\"]' | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_categories",
            "TOPIC   | 2 | '{}'                      | 추리 소설 | '[0.1]'      | 1 | ck_user_memories_categories",
            "TOPIC   | 2 | '[]'                      | ' '       | '[0.1]'      | 1 | ck_user_memories_keyword_not_blank",
            "TOPIC   | 2 | '[]'                      | 추리 소설 | '[0.1, 0.2]' | 1 | ck_user_memories_vector"})
    void DB_제약을_어기는_기억은_저장하지_않는다(final String type, final int strength, final String categories, final String keyword,
        final String vector, final int dim, final String constraint) {
        final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", "제약 검증 회원");
        final Long userId = jdbc.queryForObject("SELECT MAX(id) FROM users", Long.class);
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO user_memories (user_id, type, value, strength, categories, keyword, vector, dim, conversation_id)
            VALUES (?, ?, '기억 문장', ?, CAST(? AS JSON), ?, CAST(? AS JSON), ?, 'cv_1')
            """, userId, type, strength, categories, keyword, vector, dim))
            .isInstanceOfSatisfying(UncategorizedSQLException.class, (final UncategorizedSQLException exception) -> {
                final SQLException cause = exception.getSQLException();
                org.assertj.core.api.Assertions.assertThat(cause.getErrorCode()).isEqualTo(3819);
                org.assertj.core.api.Assertions.assertThat(cause.getMessage()).contains(constraint);
            });
    }
}
