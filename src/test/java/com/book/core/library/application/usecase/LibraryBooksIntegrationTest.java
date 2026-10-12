package com.book.core.library.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.library.application.port.UserLibraryBookRepositoryPort;
import com.book.core.library.domain.UserLibraryBook;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class LibraryBooksIntegrationTest {
    private static final LocalDateTime ADDED_AT = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UserLibraryBookRepositoryPort libraryBookRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 같은_회원이_같은_도서를_다시_저장하면_UNIQUE_제약으로_DuplicateKeyException이_발생한다() {
        final Long userId = insertUser("도서관중복회원");
        final Long bookId = insertBook(910001L);
        libraryBookRepository.saveAll(List.of(UserLibraryBook.add(userId, bookId, ADDED_AT)));

        assertThatThrownBy(() -> libraryBookRepository.saveAll(List.of(UserLibraryBook.add(userId, bookId, ADDED_AT))))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 존재하지_않는_도서를_저장하면_FK_제약_위반은_DuplicateKeyException이_아니다() {
        final Long userId = insertUser("도서관FK회원");

        assertThatThrownBy(() -> libraryBookRepository.saveAll(List.of(UserLibraryBook.add(userId, 999999999L, ADDED_AT))))
            .isInstanceOf(DataIntegrityViolationException.class).isNotInstanceOf(DuplicateKeyException.class);
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
