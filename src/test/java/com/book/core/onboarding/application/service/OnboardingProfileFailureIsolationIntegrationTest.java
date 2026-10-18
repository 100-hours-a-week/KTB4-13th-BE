package com.book.core.onboarding.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OnboardingProfileFailureIsolationIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OnboardingService onboardingService;

    @MockitoBean
    TermRepositoryPort termRepository;

    @MockitoSpyBean
    AddLibraryBooksUseCase addLibraryBooksUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 온보딩_완료가_커밋된_뒤_AI_취향_프로필_생성이_실패해도_완료_요청은_성공한다() {
        final Long userId = insertUser("프로필실패격리회원");
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        when(termRepository.findActiveByTermType(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatCode(() -> onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of()))).doesNotThrowAnyException();

        assertThat(jdbc.queryForObject("SELECT onboarding_status FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL",
            String.class, userId)).isEqualTo("COMPLETED");
    }

    @Test
    void 최초_완료한_도서만_도서관에_추가하고_완료_후_재요청은_도서관을_변경하지_않는다() {
        final Long userId = startOnboarding("도서관연동회원");
        final Long firstBookId = insertBook(920001L);

        onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of(firstBookId)));
        assertThat(activeLibraryBookIds(userId)).containsExactly(firstBookId);

        jdbc.update("UPDATE user_library_books SET deleted_at = NOW(6) WHERE user_id = ?", userId);
        onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of(insertBook(920002L))));
        assertThat(activeLibraryBookIds(userId)).isEmpty();
    }

    @Test
    void 도서관_추가가_최종_실패해도_커밋된_온보딩_완료와_도서는_유지된다() {
        final Long userId = startOnboarding("도서관실패격리회원");
        final Long bookId = insertBook(920101L);
        doThrow(new QueryTimeoutException("timeout")).when(addLibraryBooksUseCase).execute(any());

        assertThatCode(() -> onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of(bookId))))
            .doesNotThrowAnyException();

        assertThat(jdbc.queryForObject("SELECT onboarding_status FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL",
            String.class, userId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForList("SELECT book_id FROM user_onboarding_books WHERE user_id = ?", Long.class, userId))
            .containsExactly(bookId);
        assertThat(activeLibraryBookIds(userId)).isEmpty();
    }

    @Test
    void 같은_회원의_동시_완료_요청은_한_요청만_최초_완료로_처리해_그_도서만_도서관에_추가한다() throws Exception {
        final Long userId = startOnboarding("동시완료회원");
        final List<Long> bookIds = List.of(insertBook(920201L), insertBook(920202L), insertBook(920203L), insertBook(920204L));
        final CyclicBarrier barrier = new CyclicBarrier(bookIds.size());
        final List<Callable<Void>> requests = bookIds.stream().map((final Long bookId) -> (Callable<Void>) () -> {
            barrier.await();
            onboardingService.saveBooks(new PutOnboardingBooksCommand(userId, List.of(bookId)));
            return null;
        }).toList();

        final ExecutorService executor = Executors.newFixedThreadPool(bookIds.size());
        try {
            for (final Future<Void> result : executor.invokeAll(requests)) {
                assertThatCode(result::get).doesNotThrowAnyException();
            }
        } finally {
            executor.shutdownNow();
        }

        verify(addLibraryBooksUseCase, times(1)).execute(any());
        assertThat(activeLibraryBookIds(userId)).hasSize(1).isSubsetOf(bookIds);
    }

    private Long startOnboarding(final String nickname) {
        final Long userId = insertUser(nickname);
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        return userId;
    }

    private Long insertBook(final Long bookId) {
        jdbc.update(
            "INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, '제목', '저자', '출판사', '소설', '2026-01-01')",
            bookId);
        return bookId;
    }

    private List<Long> activeLibraryBookIds(final Long userId) {
        return jdbc.queryForList("SELECT book_id FROM user_library_books WHERE user_id = ? AND deleted_at IS NULL", Long.class, userId);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }
}
