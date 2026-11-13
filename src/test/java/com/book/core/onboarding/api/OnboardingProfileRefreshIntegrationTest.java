package com.book.core.onboarding.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.book.core.onboarding.application.usecase.ProcessProfileSyncJobUseCase;
import com.book.core.onboarding.application.usecase.RequestProfileSyncUseCase;
import com.book.core.onboarding.application.usecase.SaveOnboardingAnswersUseCase;
import com.book.core.user.application.service.UserService;
import com.book.core.user.application.usecase.CompleteUserDeletionUseCase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "app.profile-sync.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class OnboardingProfileRefreshIntegrationTest {
    private static final String LUNCH = "점심시간이나 짧은 휴식 시간";
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    MockMvc mvc;

    @Autowired
    ProcessProfileSyncJobUseCase processJob;

    @Autowired
    SaveOnboardingAnswersUseCase saveOnboardingAnswers;

    @Autowired
    UserService userService;

    @MockitoSpyBean
    RequestProfileSyncUseCase requestProfileSyncUseCase;

    @MockitoSpyBean
    CompleteUserDeletionUseCase completeUserDeletionUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    DataSource dataSource;

    private Connection workerLock;

    @MockitoBean
    AiPersonalizationProfileClient profileClient;

    private final List<AiPersonalizationProfileRequest> updateRequests = new ArrayList<>();
    private final List<List<Long>> committedOptionIdsAtUpdate = new ArrayList<>();
    private final List<List<Long>> committedBookIdsAtUpdate = new ArrayList<>();

    @BeforeEach
    void recordProfileRequests() throws SQLException {
        lockWorker();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            final AiPersonalizationProfileRequest request = invocation.getArgument(0);
            updateRequests.add(request);
            committedOptionIdsAtUpdate.add(readOnNewConnection(() -> optionIds(request.userId())));
            committedBookIdsAtUpdate.add(readOnNewConnection(() -> bookIds(request.userId())));
            return null;
        }).when(profileClient).updateProfile(any(), any());
    }

    @AfterEach
    void clearSecurityContext() throws SQLException {
        SecurityContextHolder.clearContext();
        try {
            jdbc.update("DELETE FROM profile_sync_jobs");
        } finally {
            unlockWorker();
        }
    }

    @Test
    void 예약된_작업은_스케줄러가_자동으로_처리한다() throws Exception {
        final Long userId = insertUser("프로필갱신회원6");
        final Long bookId = insertBook("프로필 갱신 도서 7");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);

        verifyNoInteractions(profileClient);
        unlockWorker();
        verify(profileClient, timeout(10000)).updateProfile(any(), any());
        lockWorker();
        assertThat(jobCount(userId)).isZero();
    }

    private void lockWorker() throws SQLException {
        workerLock = dataSource.getConnection();
        try (var statement = workerLock.createStatement();
            var result = statement.executeQuery("SELECT GET_LOCK('book_profile_sync', 10)")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isOne();
        }
    }

    private void unlockWorker() throws SQLException {
        if (workerLock == null) {
            return;
        }
        try (Connection connection = workerLock; var statement = connection.createStatement()) {
            statement.executeQuery("SELECT RELEASE_LOCK('book_profile_sync')").close();
        } finally {
            workerLock = null;
        }
    }

    @Test
    void 온보딩은_AI를_기다리지_않고_작업자가_커밋된_최신_선호를_한_번_반영한다() throws Exception {
        final Long userId = insertUser("프로필갱신회원1");
        final Long firstBookId = insertBook("프로필 갱신 도서 1");
        final Long secondBookId = insertBook("프로필 갱신 도서 2");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(firstBookId);
        updatePreferences(2, secondBookId).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verifyNoInteractions(profileClient);
        assertThat(jobCount(userId)).isOne();
        processJob.execute();

        assertThat(updateRequests).singleElement().satisfies(request -> {
            assertThat(request.readingTimes()).containsExactly(LUNCH);
            assertThat(request.likedBookIds()).containsExactly(secondBookId);
        });
        assertThat(committedOptionIdsAtUpdate.getLast()).contains(2L).doesNotContain(1L);
        assertThat(committedBookIdsAtUpdate.getLast()).containsExactly(secondBookId);
        assertThat(jobCount(userId)).isZero();

        updatePreferences(2, secondBookId).andExpect(status().isOk());
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void AI_장애_뒤_사용자_재요청_없이_프로필을_다시_보낸다() throws Exception {
        final Long userId = insertUser("프로필갱신회원2");
        final Long bookId = insertBook("프로필 갱신 도서 3");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);

        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).doNothing().when(profileClient).updateProfile(any(), any());
        processJob.execute();
        assertThat(jobCount(userId)).isOne();
        assertThat(jdbc.queryForObject("SELECT attempts FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId)).isOne();
        processJob.execute();
        verify(profileClient).updateProfile(any(), any());

        jdbc.update("UPDATE profile_sync_jobs SET available_at = '2000-01-01' WHERE user_id = ?", userId);
        processJob.execute();
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void AI_반영_중_새_선호가_저장되면_기존_작업의_완료가_새_작업을_지우지_않는다() throws Exception {
        final Long userId = insertUser("프로필갱신회원3");
        final Long bookId = insertBook("프로필 갱신 도서 4");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);
        doAnswer(invocation -> {
            updatePreferences(2, bookId).andExpect(status().isOk());
            return null;
        }).doAnswer(invocation -> {
            updateRequests.add(invocation.getArgument(0));
            return null;
        }).when(profileClient).updateProfile(any(), any());

        processJob.execute();
        assertThat(jobCount(userId)).isOne();
        processJob.execute();
        assertThat(updateRequests).singleElement().satisfies(request -> assertThat(request.readingTimes()).containsExactly(LUNCH));
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void 동의가_없으면_예약하지_않고_예약_뒤_동의가_없어져도_AI를_부르지_않는다() throws Exception {
        final Long userId = insertUser("프로필갱신회원4");
        final Long bookId = insertBook("프로필 갱신 도서 5");
        authenticateAs(userId);
        completeOnboarding(bookId);
        assertThat(jobCount(userId)).isZero();
        agreePersonalization();
        updatePreferences(2, bookId).andExpect(status().isOk());
        assertThat(jobCount(userId)).isOne();

        jdbc.update("DELETE FROM user_term_agreement WHERE user_id = ?", userId);
        processJob.execute();
        verify(profileClient, never()).updateProfile(any(), any());
        verify(profileClient).deleteProfile(eq(userId), any());
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void PUT_도중_동의_행이_없어지면_PUT_다음_DELETE를_보낸다() throws Exception {
        final Long userId = insertUser("프로필갱신회원7");
        final Long bookId = insertBook("프로필 갱신 도서 8");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);
        doAnswer(invocation -> {
            jdbc.update("DELETE FROM user_term_agreement WHERE user_id = ?", userId);
            return null;
        }).when(profileClient).updateProfile(any(), any());

        processJob.execute();
        assertThat(jobCount(userId)).isOne();
        processJob.execute();

        final InOrder order = inOrder(profileClient);
        order.verify(profileClient).updateProfile(any(), any());
        order.verify(profileClient).deleteProfile(eq(userId), any());
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void 복구_기간이_끝나_최종_탈퇴하면_AI_프로필을_삭제하고_AI_장애_뒤에도_회원_ID로_다시_삭제한다() throws Exception {
        final Long userId = insertUser("최종탈퇴회원1");
        final Long bookId = insertBook("최종 탈퇴 도서 1");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);
        processJob.execute();
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now().minusDays(8), userId);

        userService.completeUserDeletion();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, userId))
            .isOne();
        assertThat(jobCount(userId)).isOne();
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).doNothing().when(profileClient).deleteProfile(eq(userId), any());
        processJob.execute();
        assertThat(jobCount(userId)).isOne();
        jdbc.update("UPDATE profile_sync_jobs SET available_at = '2000-01-01' WHERE user_id = ?", userId);
        processJob.execute();

        verify(profileClient, times(2)).deleteProfile(eq(userId), any());
        verify(profileClient, times(1)).updateProfile(any(), any());
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void AI가_삭제_요청을_400으로_거부하면_작업을_실패로_보관하고_운영자가_다시_열면_삭제한다() throws Exception {
        final Long userId = insertUser("최종탈퇴회원2");
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now().minusDays(8), userId);
        userService.completeUserDeletion();
        doThrow(new CoreException(ErrorCode.AI_SERVICE_INVALID_REQUEST)).doNothing().when(profileClient).deleteProfile(eq(userId), any());

        processJob.execute();
        processJob.execute();

        verify(profileClient, times(1)).deleteProfile(eq(userId), any());
        assertThat(jobCount(userId)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ? AND available_at >= '9999-01-01'",
            Integer.class, userId)).isOne();

        jdbc.update("UPDATE profile_sync_jobs SET available_at = NOW(6), attempts = 0 WHERE user_id = ? AND available_at >= '9999-01-01'",
            userId);
        processJob.execute();

        verify(profileClient, times(2)).deleteProfile(eq(userId), any());
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void 삭제_예약이나_최종_탈퇴_커밋_사이에_중단돼도_다음_실행에서_회원을_다시_찾아_삭제를_예약한다() {
        final Long beforeEnqueueUserId = insertUser("등록전중단회원");
        final Long beforeCompleteUserId = insertUser("완료전중단회원");
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id IN (?, ?)", LocalDateTime.now().minusDays(8), beforeEnqueueUserId,
            beforeCompleteUserId);
        doThrow(new IllegalStateException("shutdown before enqueue")).doCallRealMethod().when(requestProfileSyncUseCase)
            .execute(beforeEnqueueUserId);
        doThrow(new IllegalStateException("shutdown before completion")).doCallRealMethod().when(completeUserDeletionUseCase)
            .execute(beforeCompleteUserId);

        userService.completeUserDeletion();

        assertThat(deletedUserCount(beforeEnqueueUserId)).isZero();
        assertThat(jobCount(beforeEnqueueUserId)).isZero();
        assertThat(deletedUserCount(beforeCompleteUserId)).isZero();
        assertThat(jobCount(beforeCompleteUserId)).isOne();

        userService.completeUserDeletion();

        assertThat(deletedUserCount(beforeEnqueueUserId)).isOne();
        assertThat(jobCount(beforeEnqueueUserId)).isOne();
        assertThat(deletedUserCount(beforeCompleteUserId)).isOne();
        assertThat(jobCount(beforeCompleteUserId)).isOne();
    }

    @Test
    void 복구_기간_중이거나_탈퇴를_취소한_회원은_AI_프로필을_삭제하지_않는다() throws Exception {
        final Long pendingUserId = insertUser("탈퇴유예회원1");
        final Long recoveredUserId = insertUser("탈퇴취소회원1");
        final Long bookId = insertBook("탈퇴 유예 도서 1");
        for (final Long userId : List.of(pendingUserId, recoveredUserId)) {
            authenticateAs(userId);
            agreePersonalization();
            completeOnboarding(bookId);
        }
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now().minusDays(6), pendingUserId);
        jdbc.update("UPDATE users SET delete_requested_at = NULL WHERE id = ?", recoveredUserId);

        userService.completeUserDeletion();
        processJob.execute();
        processJob.execute();

        verify(profileClient, never()).deleteProfile(any(), any());
        verify(profileClient).updateProfile(any(), any());
        assertThat(jobCount(pendingUserId)).isZero();
        assertThat(jobCount(recoveredUserId)).isZero();
    }

    @Test
    void 온보딩_변경이_롤백되면_예약도_롤백된다() throws Exception {
        final Long userId = insertUser("프로필갱신회원5");
        final Long bookId = insertBook("프로필 갱신 도서 6");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);
        processJob.execute();

        transaction.executeWithoutResult(tx -> {
            assertThatCode(() -> updatePreferences(2, bookId).andExpect(status().isOk())).doesNotThrowAnyException();
            assertThat(jobCount(userId)).isOne();
            tx.setRollbackOnly();
        });

        assertThat(optionIds(userId)).contains(1L).doesNotContain(2L);
        assertThat(jobCount(userId)).isZero();
    }

    @Test
    void Q3_변경을_기다린_Q4_저장은_해제된_부모의_답변을_거절한다() throws Exception {
        final Long userId = insertUser("온보딩답변동시저장회원");
        saveOnboardingAnswers.execute(new PutOnboardingAnswersCommand(userId, 3L, List.of(9L, 10L)));
        saveOnboardingAnswers.execute(new PutOnboardingAnswersCommand(userId, 4L, List.of(23L, 28L)));
        final CountDownLatch q3Saved = new CountDownLatch(1);
        final CountDownLatch commitQ3 = new CountDownLatch(1);
        final CountDownLatch q4Started = new CountDownLatch(1);
        final AtomicLong q3ConnectionId = new AtomicLong();
        final AtomicLong q4ConnectionId = new AtomicLong();

        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var q3 = executor.submit(() -> {
                transaction.executeWithoutResult(status -> {
                    q3ConnectionId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                    saveOnboardingAnswers.execute(new PutOnboardingAnswersCommand(userId, 3L, List.of(10L)));
                    q3Saved.countDown();
                    try {
                        assertThat(commitQ3.await(20, TimeUnit.SECONDS)).isTrue();
                    } catch (final InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                });
            });
            try {
                assertThat(q3Saved.await(5, TimeUnit.SECONDS)).isTrue();
                final var q4 = executor.submit(() -> {
                    transaction.executeWithoutResult(status -> {
                        q4ConnectionId.set(jdbc.queryForObject("SELECT CONNECTION_ID()", Long.class));
                        q4Started.countDown();
                        saveOnboardingAnswers.execute(new PutOnboardingAnswersCommand(userId, 4L, List.of(23L)));
                    });
                });
                assertThat(q4Started.await(5, TimeUnit.SECONDS)).isTrue();
                assertOnboardingLockWait(q3ConnectionId.get(), q4ConnectionId.get());
                commitQ3.countDown();

                q3.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> q4.get(5, TimeUnit.SECONDS)).cause().isInstanceOfSatisfying(CoreException.class,
                    exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED));
                assertThat(optionIds(userId)).containsExactlyInAnyOrder(10L, 28L);
            } finally {
                commitQ3.countDown();
            }
        }
    }

    private void assertOnboardingLockWait(final long holderConnectionId, final long waiterConnectionId) throws SQLException {
        final String sql = """
            SELECT COUNT(*) FROM performance_schema.data_lock_waits w
            JOIN performance_schema.data_locks requested ON w.REQUESTING_ENGINE_LOCK_ID = requested.ENGINE_LOCK_ID
                AND w.ENGINE = requested.ENGINE
            JOIN performance_schema.threads waiter ON w.REQUESTING_THREAD_ID = waiter.THREAD_ID
            JOIN performance_schema.threads holder ON w.BLOCKING_THREAD_ID = holder.THREAD_ID
            WHERE requested.OBJECT_SCHEMA = DATABASE() AND requested.OBJECT_NAME = 'user_onboardings'
                AND holder.PROCESSLIST_ID = ? AND waiter.PROCESSLIST_ID = ?
            """;
        try (final var connection = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
            final var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, holderConnectionId);
            statement.setLong(2, waiterConnectionId);
            await().atMost(Duration.ofSeconds(5)).until(() -> {
                try (final var result = statement.executeQuery()) {
                    return result.next() && result.getInt(1) > 0;
                }
            });
        }
    }

    private int deletedUserCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, userId);
    }

    private int jobCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM profile_sync_jobs WHERE user_id = ?", Integer.class, userId);
    }

    private void completeOnboarding(final Long bookId) throws Exception {
        saveAnswers(1, "[1]");
        saveAnswers(2, "[6]");
        saveAnswers(3, "[9]");
        saveAnswers(4, "[23]");
        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[" + bookId + "]}"))
            .andExpect(status().isOk());
    }

    private ResultActions updatePreferences(final long readingTimeOptionId, final Long bookId) throws Exception {
        return mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content("""
            {"answers":[{"questionId":1,"optionIds":[%d]},{"questionId":2,"optionIds":[6]},
            {"questionId":3,"optionIds":[9]},{"questionId":4,"optionIds":[23]}],"bookIds":[%d]}
            """.formatted(readingTimeOptionId, bookId)));
    }

    private void agreePersonalization() throws Exception {
        mvc.perform(post("/api/v1/onboarding/personalization-agreement")).andExpect(status().isOk());
    }

    private void saveAnswers(final int questionId, final String optionIds) throws Exception {
        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", questionId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":" + optionIds + "}")).andExpect(status().isOk());
    }

    private static List<Long> readOnNewConnection(final Supplier<List<Long>> query) {
        return CompletableFuture.supplyAsync(query).join();
    }

    private List<Long> optionIds(final Long userId) {
        return jdbc.queryForList("SELECT onboarding_option_id FROM user_onboarding_answers WHERE user_id = ?", Long.class, userId);
    }

    private List<Long> bookIds(final Long userId) {
        return jdbc.queryForList("SELECT book_id FROM user_onboarding_books WHERE user_id = ?", Long.class, userId);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertBook(final String title) {
        jdbc.update("INSERT INTO books (title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?)", title, "저자", "출판사",
            "카테고리", LocalDate.of(2026, 1, 1));
        return jdbc.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
