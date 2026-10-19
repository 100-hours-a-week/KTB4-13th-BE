package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql"})
@Testcontainers
@Tag("integration")
class UpdateOnboardingPreferencesIntegrationTest {
    private static final long MORNING_OPTION_ID = 1L;
    private static final long LUNCH_OPTION_ID = 2L;
    private static final long PUBLISHER_OPTION_ID = 6L;
    private static final long NOVEL_OPTION_ID = 9L;
    private static final long HUMANITIES_OPTION_ID = 10L;
    private static final long NOVEL_THRILLER_OPTION_ID = 23L;
    private static final long HUMANITIES_PSYCHOLOGY_OPTION_ID = 28L;
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 1, 1, 9, 0, 0, 123_456_000);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UpdateOnboardingPreferencesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    UserOnboardingBookRepositoryPort bookRepository;

    @Test
    void 변경된_답변과_도서만_삭제_추가하고_유지된_행은_그대로_둔다() {
        final Long userId = completedUser("전체수정회원1");
        final Long removedBookId = insertBook("전체 수정 도서 1");
        final Long keptBookId = insertBook("전체 수정 도서 2");
        final Long addedBookId = insertBook("전체 수정 도서 3");
        insertAnswers(userId, List.of(MORNING_OPTION_ID, PUBLISHER_OPTION_ID, NOVEL_OPTION_ID, NOVEL_THRILLER_OPTION_ID));
        insertBooks(userId, List.of(removedBookId, keptBookId));
        final Map<Long, Long> answerRowIdsBefore = answerRowIds(userId);
        final Map<Long, Long> bookRowIdsBefore = bookRowIds(userId);

        useCase.execute(new PutOnboardingPreferencesCommand(userId, List.of(answer(1L, LUNCH_OPTION_ID), answer(2L, PUBLISHER_OPTION_ID),
            answer(3L, NOVEL_OPTION_ID, HUMANITIES_OPTION_ID), answer(4L, NOVEL_THRILLER_OPTION_ID, HUMANITIES_PSYCHOLOGY_OPTION_ID)),
            List.of(keptBookId, addedBookId)));

        final Map<Long, Long> answerRowIdsAfter = answerRowIds(userId);
        assertThat(answerRowIdsAfter).containsOnlyKeys(LUNCH_OPTION_ID, PUBLISHER_OPTION_ID, NOVEL_OPTION_ID, HUMANITIES_OPTION_ID,
            NOVEL_THRILLER_OPTION_ID, HUMANITIES_PSYCHOLOGY_OPTION_ID);
        for (final long keptOptionId : List.of(PUBLISHER_OPTION_ID, NOVEL_OPTION_ID, NOVEL_THRILLER_OPTION_ID)) {
            assertThat(answerRowIdsAfter.get(keptOptionId)).isEqualTo(answerRowIdsBefore.get(keptOptionId));
        }
        final Map<Long, Long> bookRowIdsAfter = bookRowIds(userId);
        assertThat(bookRowIdsAfter).containsOnlyKeys(keptBookId, addedBookId);
        assertThat(bookRowIdsAfter.get(keptBookId)).isEqualTo(bookRowIdsBefore.get(keptBookId));
        assertThat(jdbc.queryForObject("SELECT onboarding_status FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL",
            String.class, userId)).isEqualTo("COMPLETED");
        assertThat(completedAt(userId)).isEqualTo(COMPLETED_AT);
    }

    @Test
    void 답변_변경_후_도서_저장에_실패하면_답변과_도서를_모두_기존_상태로_되돌린다() {
        final Long userId = completedUser("전체수정롤백회원1");
        final Long removedBookId = insertBook("전체 수정 롤백 도서 1");
        final Long keptBookId = insertBook("전체 수정 롤백 도서 2");
        final Long addedBookId = insertBook("전체 수정 롤백 도서 3");
        insertAnswers(userId, List.of(MORNING_OPTION_ID, PUBLISHER_OPTION_ID, NOVEL_OPTION_ID, NOVEL_THRILLER_OPTION_ID));
        insertBooks(userId, List.of(removedBookId, keptBookId));
        final Map<Long, Long> answerRowIdsBefore = answerRowIds(userId);
        final Map<Long, Long> bookRowIdsBefore = bookRowIds(userId);
        doThrow(new DataAccessResourceFailureException("도서 저장 실패")).when(bookRepository).saveAll(anyList());

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingPreferencesCommand(userId,
            List.of(answer(1L, LUNCH_OPTION_ID), answer(2L, PUBLISHER_OPTION_ID), answer(3L, NOVEL_OPTION_ID, HUMANITIES_OPTION_ID),
                answer(4L, NOVEL_THRILLER_OPTION_ID, HUMANITIES_PSYCHOLOGY_OPTION_ID)),
            List.of(keptBookId, addedBookId)))).isInstanceOf(DataAccessResourceFailureException.class);

        verify(bookRepository).deleteByUserIdAndBookIdIn(userId, List.of(removedBookId));
        assertThat(answerRowIds(userId)).isEqualTo(answerRowIdsBefore);
        assertThat(bookRowIds(userId)).isEqualTo(bookRowIdsBefore);
        assertThat(completedAt(userId)).isEqualTo(COMPLETED_AT);
    }

    private OnboardingPreferenceAnswerCommand answer(final Long questionId, final Long... optionIds) {
        return new OnboardingPreferenceAnswerCommand(questionId, List.of(optionIds));
    }

    private Long completedUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        final Long userId = jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status, completed_at) VALUES (?, 'COMPLETED', ?)", userId,
            COMPLETED_AT);
        return userId;
    }

    private LocalDateTime completedAt(final Long userId) {
        return jdbc.queryForObject("SELECT completed_at FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL",
            LocalDateTime.class, userId);
    }

    private Long insertBook(final String title) {
        jdbc.update("INSERT INTO books (title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?)", title, "저자", "출판사",
            "카테고리", LocalDate.of(2026, 1, 1));
        return jdbc.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }

    private void insertAnswers(final Long userId, final List<Long> optionIds) {
        optionIds.forEach(
            optionId -> jdbc.update("INSERT INTO user_onboarding_answers (user_id, onboarding_option_id) VALUES (?, ?)", userId, optionId));
    }

    private void insertBooks(final Long userId, final List<Long> bookIds) {
        bookIds.forEach(bookId -> jdbc.update("INSERT INTO user_onboarding_books (user_id, book_id) VALUES (?, ?)", userId, bookId));
    }

    private Map<Long, Long> answerRowIds(final Long userId) {
        return jdbc.queryForList("SELECT onboarding_option_id, id FROM user_onboarding_answers WHERE user_id = ?", userId).stream().collect(
            Collectors.toMap(row -> ((Number) row.get("onboarding_option_id")).longValue(), row -> ((Number) row.get("id")).longValue()));
    }

    private Map<Long, Long> bookRowIds(final Long userId) {
        return jdbc.queryForList("SELECT book_id, id FROM user_onboarding_books WHERE user_id = ?", userId).stream()
            .collect(Collectors.toMap(row -> ((Number) row.get("book_id")).longValue(), row -> ((Number) row.get("id")).longValue()));
    }
}
