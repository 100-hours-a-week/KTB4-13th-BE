package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class DeleteUserMemoriesUseCasesIntegrationTest {
    private static final String DELETED_AT = "2026-10-01 10:00:00";

    @Autowired
    DeleteUserMemoriesUseCase deleteUserMemoriesUseCase;

    @Autowired
    DeleteAllUserMemoriesUseCase deleteAllUserMemoriesUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    Long userId;

    Long otherUserId;

    Long memory101;

    Long memory102;

    Long memory103;

    Long memory105;

    Long memory201;

    @BeforeEach
    void setUp() {
        userId = insertUser("기억삭제회원");
        otherUserId = insertUser("기억삭제타인");
        memory101 = insertMemory(userId, null);
        memory102 = insertMemory(userId, null);
        memory103 = insertMemory(userId, null);
        memory105 = insertMemory(userId, DELETED_AT);
        memory201 = insertMemory(otherUserId, null);
    }

    @Test
    void 선택한_본인_활성_기억만_중복_없이_Soft_Delete한다() {
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(userId, List.of(memory101)))).isEqualTo(1);
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(userId, List.of(memory102, memory103, memory102))))
            .isEqualTo(2);

        assertThat(activeIds()).containsExactly(memory201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_memories WHERE user_id IN (?, ?)", Integer.class, userId, otherUserId))
            .isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(longs = {201L, 999L})
    void 다른_회원의_기억이나_없는_기억이_있으면_아무것도_삭제하지_않고_404를_던진다(final long invalidCase) {
        // 201은 다른 회원의 기억, 999는 존재하지 않는 기억입니다.
        final long invalidId = invalidCase == 201L ? memory201 : Long.MAX_VALUE;
        assertThatThrownBy(() -> deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(userId, List.of(memory101, invalidId))))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_MEMORY_NOT_FOUND));

        assertThat(activeIds()).containsExactly(memory101, memory102, memory103, memory201);
    }

    @Test
    void 이미_삭제한_본인_기억은_삭제_시각을_유지하고_성공한다() {
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(userId, List.of(memory105)))).isZero();

        assertThat(deletedAt(memory105)).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    void 전체_삭제는_본인_활성_기억만_지우고_다른_회원_기억과_온보딩과_기존_삭제_기록을_유지한다() {
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status, completed_at) VALUES (?, 'COMPLETED', CURRENT_TIMESTAMP(6))",
            userId);

        assertThat(deleteAllUserMemoriesUseCase.execute(userId)).isEqualTo(3);

        assertThat(activeIds()).containsExactly(memory201);
        assertThat(deletedAt(memory105)).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL", Integer.class, userId))
            .isEqualTo(1);
    }

    @Test
    void 전체_삭제할_활성_기억이_없어도_성공한다() {
        deleteAllUserMemoriesUseCase.execute(userId);

        assertThat(deleteAllUserMemoriesUseCase.execute(userId)).isZero();
    }

    @Test
    void 같은_회원의_기억_저장이_진행_중이면_선택_삭제와_전체_삭제는_커밋을_기다린다() throws Exception {
        assertWaitsForUserLock(() -> deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(userId, List.of(memory101))));
        assertWaitsForUserLock(() -> deleteAllUserMemoriesUseCase.execute(userId));

        assertThat(activeIds()).containsExactly(memory201);
    }

    private void assertWaitsForUserLock(final Supplier<Integer> deletion) throws Exception {
        final CountDownLatch locked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CompletableFuture<Void> saving =
            CompletableFuture.runAsync(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
                jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
                locked.countDown();
                await(release);
            }));
        await(locked);

        final CompletableFuture<Integer> deleting = CompletableFuture.supplyAsync(deletion);
        Thread.sleep(500);
        final boolean finishedWhileLocked = deleting.isDone();
        release.countDown();
        saving.get(10, TimeUnit.SECONDS);
        deleting.get(10, TimeUnit.SECONDS);

        assertThat(finishedWhileLocked).isFalse();
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간을 초과했습니다");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private List<Long> activeIds() {
        return jdbc.queryForList("SELECT id FROM user_memories WHERE user_id IN (?, ?) AND deleted_at IS NULL ORDER BY id", Long.class,
            userId, otherUserId);
    }

    private LocalDateTime deletedAt(final Long id) {
        return jdbc.queryForObject("SELECT deleted_at FROM user_memories WHERE id = ?", LocalDateTime.class, id);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertMemory(final Long userId, final String deletedAt) {
        jdbc.update("""
            INSERT INTO user_memories (user_id, type, value, strength, categories, keyword, vector, dim, conversation_id, deleted_at)
            VALUES (?, 'TOPIC', '경제 책을 좋아해요', 2, '[]', '경제', '[0.1]', 1, 'cv_1', ?)
            """, userId, deletedAt);
        return jdbc.queryForObject("SELECT MAX(id) FROM user_memories WHERE user_id = ?", Long.class, userId);
    }
}
