package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.domain.UserMemoryType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class SaveUserMemoriesUseCaseIntegrationTest {
    private static final long USER_ID = 9001L;
    private static final String CONVERSATION_ID = "cv_20261013_a1";

    @Autowired
    SaveUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM user_memories");
        jdbc.update("DELETE FROM users");
        jdbc.update("""
            INSERT INTO users (id, nickname, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, USER_ID, "북적이");
    }

    @Test
    void 최초_추출_결과를_대화와_함께_저장한다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of("추리/스릴러"))));

        final var row = jdbc.queryForMap("SELECT * FROM user_memories");
        assertThat(row).containsEntry("user_id", USER_ID).containsEntry("type", "TOPIC").containsEntry("keyword", "추리 소설")
            .containsEntry("strength", 2).containsEntry("dim", 2).containsEntry("conversation_id", CONVERSATION_ID);
        assertThat(row.get("categories")).isEqualTo("[\"추리/스릴러\"]");
        assertThat(row.get("vector")).isEqualTo("[0.054, -0.25]");
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void 같은_대화를_재추출하면_활성_기억만_교체하고_삭제_기억과_같은_결과는_저장하지_않는다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, "김영하", List.of())));
        useCase.execute(command("cv_other", extraction(UserMemoryType.MOOD, "잔잔한", List.of())));
        jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE keyword = '김영하'");

        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "로맨스 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, " 김영하 ", List.of()), extraction(UserMemoryType.TOPIC, "김영하", List.of())));

        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).containsExactlyInAnyOrder("로맨스 소설", "김영하");
        assertThat(jdbc.queryForList("SELECT type FROM user_memories WHERE deleted_at IS NULL AND keyword = '김영하'", String.class))
            .containsExactly("TOPIC");
        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NOT NULL")).containsExactly("김영하");
        assertThat(keywords("cv_other", "deleted_at IS NULL")).containsExactly("잔잔한");
    }

    @Test
    void 같은_회원의_삭제가_진행_중이면_재추출은_삭제_커밋을_기다린_뒤_삭제_기억을_다시_저장하지_않는다() throws Exception {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.AUTHOR, "김영하", List.of())));
        final CountDownLatch deleting = new CountDownLatch(1);
        final CountDownLatch commitDelete = new CountDownLatch(1);
        final CompletableFuture<Void> deletion =
            CompletableFuture.runAsync(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final var status) -> {
                jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, USER_ID);
                jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE keyword = '김영하'");
                deleting.countDown();
                await(commitDelete);
            }));
        await(deleting);

        final CompletableFuture<Void> reExtraction = CompletableFuture
            .runAsync(() -> useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.AUTHOR, "김영하", List.of()))));
        Thread.sleep(500);
        final boolean finishedBeforeDeleteCommit = reExtraction.isDone();
        commitDelete.countDown();
        deletion.get(10, TimeUnit.SECONDS);
        reExtraction.get(10, TimeUnit.SECONDS);

        assertThat(finishedBeforeDeleteCommit).isFalse();
        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).isEmpty();
        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NOT NULL")).containsExactly("김영하");
    }

    @Test
    void 빈_추출_결과면_같은_대화의_활성_기억만_지운다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));

        useCase.execute(command(CONVERSATION_ID));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_memories", Integer.class)).isZero();
    }

    @Test
    void 새_기억_저장에_실패하면_기존_활성_기억_삭제도_되돌린다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));
        final var tooLongValue =
            new UserMemoryExtraction(UserMemoryType.TOPIC, "가".repeat(201), 2, List.of(), "로맨스 소설", List.of(0.054f, -0.25f), 2);

        assertThatThrownBy(() -> useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.MOOD, "잔잔한", List.of()), tooLongValue)))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).containsExactly("추리 소설");
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

    private SaveUserMemoriesCommand command(final String conversationId, final UserMemoryExtraction... extractions) {
        return new SaveUserMemoriesCommand(USER_ID, conversationId, List.of(extractions));
    }

    private UserMemoryExtraction extraction(final UserMemoryType type, final String keyword, final List<String> categories) {
        return new UserMemoryExtraction(type, keyword + "을 좋아함", 2, categories, keyword, List.of(0.054f, -0.25f), 2);
    }

    private List<String> keywords(final String conversationId, final String deletedCondition) {
        return jdbc.queryForList("SELECT keyword FROM user_memories WHERE conversation_id = ? AND " + deletedCondition, String.class,
            conversationId);
    }
}
