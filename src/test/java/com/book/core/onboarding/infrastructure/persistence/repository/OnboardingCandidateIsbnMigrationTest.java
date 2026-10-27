package com.book.core.onboarding.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class OnboardingCandidateIsbnMigrationTest {
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareCatalog() {
        dataSource = new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        final Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).target("65.9").load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void 제목이_달라도_ID와_ISBN이_일치하면_등록하고_재실행해도_기존_행을_보존한다() throws SQLException {
        final Long originalId = jdbc.queryForObject("SELECT id FROM onboarding_book_candidates WHERE book_id = 1156176", Long.class);
        jdbc.update("UPDATE books SET title = '변경된 제목' WHERE id IN (1968604, 2006972)");

        migrateCandidates();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM onboarding_book_candidates", Integer.class)).isEqualTo(490);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT subcategory_code) FROM onboarding_book_candidates", Integer.class))
            .isEqualTo(49);
        assertThat(jdbc.queryForObject("SELECT id FROM onboarding_book_candidates WHERE book_id = 1156176", Long.class))
            .isEqualTo(originalId);
        final var before = jdbc.queryForList("SELECT * FROM onboarding_book_candidates ORDER BY id");
        migrateCandidates();
        assertThat(jdbc.queryForList("SELECT * FROM onboarding_book_candidates ORDER BY id")).isEqualTo(before);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "9780306406157")
    void 제목이_같아도_ISBN이_없거나_다르면_후보를_추가하지_않는다(final String isbn13) {
        jdbc.update("UPDATE books SET isbn13 = ? WHERE id = 2006972", isbn13);
        final var before = jdbc.queryForList("SELECT * FROM onboarding_book_candidates ORDER BY id");

        assertThatThrownBy(this::migrateCandidates).rootCause().isInstanceOfSatisfying(SQLException.class,
            (final SQLException exception) -> assertThat(exception.getErrorCode()).isEqualTo(3819));

        assertThat(jdbc.queryForList("SELECT * FROM onboarding_book_candidates ORDER BY id")).isEqualTo(before);
    }

    private void migrateCandidates() throws SQLException {
        try (final var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V66__seed_onboarding_book_candidates.sql"));
        }
    }
}
