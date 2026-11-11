package com.book.core.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** 커밋하지 않은 INSERT로 UNIQUE 잠금을 먼저 잡고, 담기 요청이 그 잠금을 기다리는 것을 MySQL에서 확인한 뒤 커밋해 충돌을 매번 재현합니다. */
@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class LibraryAddBooksConcurrencyIntegrationTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final LocalDateTime ADDED_AT = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    LibraryService libraryService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @MockitoSpyBean
    AddLibraryBooksUseCase addLibraryBooksUseCase;

    @Test
    void 다른_트랜잭션이_같은_책을_먼저_넣으면_UNIQUE_충돌_뒤_한_번_다시_실행해_먼저_넣은_항목을_유지한다() throws Exception {
        final Long userId = insertUser("동시담기회원");
        final Long bookId = insertBook(913001L);
        final List<DuplicateKeyException> conflicts = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } catch (final DuplicateKeyException exception) {
                conflicts.add(exception);
                throw exception;
            }
        }).when(addLibraryBooksUseCase).execute(any());
        // 커넥션을 먼저 닫아 커밋하지 못한 INSERT가 롤백되면 잠금이 풀려, 실행기 종료가 담기 요청을 무한정 기다리지 않습니다.
        try (ExecutorService executor = Executors.newSingleThreadExecutor(); Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            insertUncommitted(holder, userId, bookId);

            final CompletableFuture<Void> request =
                CompletableFuture.runAsync(() -> libraryService.addBooks(new AddLibraryBooksCommand(userId, List.of(bookId))), executor);
            awaitBlockedInsert();
            assertThat(request).isNotDone();
            holder.commit();
            request.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }

        assertThat(conflicts).singleElement()
            .satisfies(conflict -> assertThat(conflict.getMostSpecificCause().getMessage()).contains("uk_user_library_books_user_book"));
        verify(addLibraryBooksUseCase, times(2)).execute(new AddLibraryBooksCommand(userId, List.of(bookId)));
        assertThat(
            jdbc.queryForList("SELECT CONCAT(added_at, ':', deleted_at IS NULL) FROM user_library_books WHERE user_id = ? AND book_id = ?",
                String.class, userId, bookId))
            .containsExactly("2026-10-01 09:00:00.000000:1");
    }

    /** 담기 요청의 INSERT가 UNIQUE 인덱스 잠금을 기다리는 상태가 될 때까지 MySQL 처리 목록을 확인합니다. */
    private void awaitBlockedInsert() {
        final long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.processlist
            WHERE id <> CONNECTION_ID() AND info LIKE 'insert into user_library_books%'
            """, Integer.class) == 0) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("담기 요청의 INSERT가 잠금을 기다리지 않았습니다.");
            }
            Thread.onSpinWait();
        }
    }

    private static void insertUncommitted(final Connection connection, final Long userId, final Long bookId) throws SQLException {
        try (PreparedStatement statement =
            connection.prepareStatement("INSERT INTO user_library_books (user_id, book_id, added_at) VALUES (?, ?, ?)")) {
            statement.setLong(1, userId);
            statement.setLong(2, bookId);
            statement.setObject(3, ADDED_AT);
            statement.executeUpdate();
        }
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertBook(final Long bookId) {
        jdbc.update(
            "INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, '제목', '저자', '출판사', '소설', '2026-01-01')",
            bookId);
        return bookId;
    }
}
