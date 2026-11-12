package com.book.core.book.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DuplicateImportedIsbnException;
import com.book.core.book.domain.Book;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class NewBookRegistrationConstraintTest {
    private static final String ISBN_A = "9788983921987";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.0").withExposedPorts(6379);

    @Autowired
    private BookRepositoryPort books;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void resetCatalog() {
        jdbc.update("DELETE FROM books WHERE imported_isbn13 = ?", ISBN_A);
    }

    @Test
    void 저장이_경합해도_동일_ISBN은_한_건만_남는다() throws Exception {
        final var ready = new CountDownLatch(2);
        final var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> trySaveConcurrently(ready, start));
            final var second = executor.submit(() -> trySaveConcurrently(ready, start));
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM books WHERE imported_isbn13 = ?", Long.class, ISBN_A)).isEqualTo(1L);
    }

    @Test
    void 기존_ISBN이_있어도_다른_저장_제약_위반은_중복으로_바꾸지_않는다() {
        transactions.executeWithoutResult((final var status) -> books.save(newBook("신간")));

        assertThatThrownBy(() -> transactions.executeWithoutResult((final var status) -> books.save(newBook("가".repeat(256)))))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM books WHERE imported_isbn13 = ?", Long.class, ISBN_A)).isEqualTo(1L);
    }

    private Book newBook(final String title) {
        return Book.registerNewBook(ISBN_A, title, "저자", null, "출판사", "800", TODAY, null);
    }

    private boolean trySaveConcurrently(final CountDownLatch ready, final CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            transactions.executeWithoutResult((final var status) -> books.save(newBook("신간")));
            return true;
        } catch (final DuplicateImportedIsbnException duplicate) {
            return false;
        }
    }
}
