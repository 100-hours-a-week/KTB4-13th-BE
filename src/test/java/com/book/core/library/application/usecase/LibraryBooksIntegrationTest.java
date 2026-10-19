package com.book.core.library.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
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
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8").withCommand("--log-bin-trust-function-creators=1");

    @Autowired
    UserLibraryBookRepositoryPort libraryBookRepository;

    @Autowired
    AddLibraryBooksUseCase addUseCase;

    @Autowired
    DeleteLibraryBooksUseCase deleteUseCase;

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
    void 도서를_추가하면_신규는_저장하고_활성_항목은_유지하며_삭제된_항목은_추가_시각을_갱신해_복구한다() {
        final Long userId = insertUser("도서관추가회원");
        final Long activeBookId = insertBook(910101L);
        final Long deletedBookId = insertBook(910102L);
        final Long newBookId = insertBook(910103L);
        insertLibraryBook(userId, activeBookId, false);
        insertLibraryBook(userId, deletedBookId, true);

        addUseCase.execute(new AddLibraryBooksCommand(userId, List.of(newBookId, activeBookId, deletedBookId, newBookId)));

        assertThat(jdbc.queryForList("""
            SELECT CONCAT(book_id, ':', deleted_at IS NULL, ':', added_at = ?) FROM user_library_books WHERE user_id = ? ORDER BY book_id
            """, String.class, ADDED_AT, userId)).containsExactly(activeBookId + ":1:1", deletedBookId + ":1:0", newBookId + ":1:0");
    }

    @Test
    void 추가_중_FK_위반이_발생하면_복구와_신규_저장을_모두_롤백한다() {
        final Long userId = insertUser("도서관추가롤백회원");
        final Long deletedBookId = insertBook(910201L);
        final Long newBookId = insertBook(910202L);
        insertLibraryBook(userId, deletedBookId, true);

        assertThatThrownBy(() -> addUseCase.execute(new AddLibraryBooksCommand(userId, List.of(deletedBookId, newBookId, 999999999L))))
            .isInstanceOf(DataIntegrityViolationException.class).isNotInstanceOf(DuplicateKeyException.class);

        assertThat(jdbc.queryForList("SELECT CONCAT(book_id, ':', deleted_at IS NULL) FROM user_library_books WHERE user_id = ?",
            String.class, userId)).containsExactly(deletedBookId + ":0");
    }

    @Test
    void 삭제는_회원_소유의_활성_항목만_처리하고_없거나_이미_삭제된_항목은_무시한다() {
        final Long userId = insertUser("도서관삭제회원");
        final Long otherUserId = insertUser("도서관타인회원");
        final Long first = insertLibraryBook(userId, insertBook(910301L), false);
        final Long second = insertLibraryBook(userId, insertBook(910302L), false);
        final Long single = insertLibraryBook(userId, insertBook(910303L), false);
        final Long alreadyDeleted = insertLibraryBook(userId, insertBook(910304L), true);
        final Long others = insertLibraryBook(otherUserId, insertBook(910305L), false);
        final String deletedAt =
            jdbc.queryForObject("SELECT deleted_at FROM user_library_books WHERE id = ?", String.class, alreadyDeleted);

        deleteUseCase.execute(new DeleteLibraryBooksCommand(userId, List.of(first, second, alreadyDeleted, others, 999999999L)));
        assertThat(activeIds(userId)).containsExactly(single);
        assertThat(activeIds(otherUserId)).containsExactly(others);

        deleteUseCase.execute(new DeleteLibraryBooksCommand(userId, List.of(single)));
        assertThat(activeIds(userId)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM user_library_books WHERE id = ?", String.class, alreadyDeleted))
            .isEqualTo(deletedAt);
    }

    @Test
    void 삭제_중_DB_오류가_발생하면_모든_삭제를_롤백한다() {
        final Long userId = insertUser("도서관삭제롤백회원");
        final Long first = insertLibraryBook(userId, insertBook(910401L), false);
        final Long second = insertLibraryBook(userId, insertBook(910402L), false);
        jdbc.execute("""
            CREATE TRIGGER fail_library_book_delete BEFORE UPDATE ON user_library_books FOR EACH ROW
            BEGIN
                IF NEW.book_id = 910402 AND NEW.deleted_at IS NOT NULL THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'delete failure';
                END IF;
            END
            """);
        try {
            assertThatThrownBy(() -> deleteUseCase.execute(new DeleteLibraryBooksCommand(userId, List.of(first, second))))
                .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER fail_library_book_delete");
        }

        assertThat(activeIds(userId)).containsExactlyInAnyOrder(first, second);
    }

    private List<Long> activeIds(final Long userId) {
        return jdbc.queryForList("SELECT id FROM user_library_books WHERE user_id = ? AND deleted_at IS NULL", Long.class, userId);
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

    private Long insertLibraryBook(final Long userId, final Long bookId, final boolean deleted) {
        jdbc.update("INSERT INTO user_library_books (user_id, book_id, added_at, deleted_at) VALUES (?, ?, ?, IF(?, NOW(6), NULL))", userId,
            bookId, ADDED_AT, deleted);
        return jdbc.queryForObject("SELECT id FROM user_library_books WHERE user_id = ? AND book_id = ?", Long.class, userId, bookId);
    }
}
