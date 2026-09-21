package com.book.core.sample.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.domain.Sample;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SampleRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    SampleRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 마이그레이션으로_생성한_테이블에_저장하고_정확한_ID로_조회한다() {
        final Sample first = repository.save(Sample.create("첫 책"));
        final Sample second = repository.save(Sample.create("둘째 책"));
        assertThat(first.id()).isPositive();
        assertThat(second.id()).isGreaterThan(first.id());
        final Sample found = repository.findById(second.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(second.id());
        assertThat(found.name()).isEqualTo("둘째 책");
        assertThat(repository.findById(Long.MAX_VALUE)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class))
                .isEqualTo(5);
    }

    @Test
    void DDL은_null과_빈_이름과_길이_초과를_거부하고_100자를_허용한다() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO samples (name) VALUES (?)", (Object) null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO samples (name) VALUES (?)", " "))
                .rootCause()
                .isInstanceOfSatisfying(
                        SQLException.class,
                        (final var exception) ->
                                assertThat(exception.getErrorCode()).isEqualTo(3819));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO samples (name) VALUES (?)", "가".repeat(101)))
                .isInstanceOf(DataIntegrityViolationException.class);
        final Sample saved = repository.save(Sample.create("가".repeat(100)));
        assertThat(repository.findById(saved.id()).orElseThrow().name()).hasSize(100);
    }
}
