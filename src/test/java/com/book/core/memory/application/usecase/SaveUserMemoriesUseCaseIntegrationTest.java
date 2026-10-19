package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.domain.UserMemoryType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class SaveUserMemoriesUseCaseIntegrationTest {
    private static final String NICKNAME = "기억추출회원";
    private static final String CONVERSATION_ID = "cv_20261013_a1";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    SaveUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    Long userId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", NICKNAME);
        userId = jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, NICKNAME);
    }

    @Test
    void 최초_추출_결과를_대화와_함께_저장한다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of("추리/스릴러"))));

        final var row = jdbc.queryForMap("SELECT * FROM user_memories WHERE user_id = ?", userId);
        assertThat(row).containsEntry("user_id", userId).containsEntry("type", "TOPIC").containsEntry("keyword", "추리 소설")
            .containsEntry("strength", 2).containsEntry("dim", 2).containsEntry("conversation_id", CONVERSATION_ID);
        assertThat(json(row.get("categories"))).isEqualTo(JSON.readTree("[\"추리/스릴러\"]"));
        assertThat(json(row.get("vector"))).isEqualTo(JSON.readTree("[0.054, -0.25]"));
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void 같은_대화를_재추출하면_활성_기억만_교체하고_삭제_기억과_같은_결과는_저장하지_않는다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, "김영하", List.of())));
        useCase.execute(command("cv_other", extraction(UserMemoryType.MOOD, "잔잔한", List.of())));
        jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ? AND keyword = '김영하'", userId);

        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "로맨스 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, " 김영하 ", List.of()), extraction(UserMemoryType.TOPIC, "김영하", List.of())));

        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).containsExactlyInAnyOrder("로맨스 소설", "김영하");
        assertThat(jdbc.queryForList("SELECT type FROM user_memories WHERE user_id = ? AND deleted_at IS NULL AND keyword = '김영하'",
            String.class, userId)).containsExactly("TOPIC");
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
                jdbc.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
                jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ? AND keyword = '김영하'", userId);
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

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_memories WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void 잘못된_새_기억은_거부하고_기존_활성_기억은_유지한다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));
        final var tooLongValue =
            new UserMemoryExtraction(UserMemoryType.TOPIC, "가".repeat(201), 2, List.of(), "로맨스 소설", List.of(0.054f, -0.25f), 2);

        assertThatThrownBy(() -> useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.MOOD, "잔잔한", List.of()), tooLongValue)))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).containsExactly("추리 소설");
    }

    // DB마다 JSON 컬럼 값의 표현과 공백이 달라 저장된 JSON 구조로 비교합니다.
    private static JsonNode json(final Object value) {
        final String text = value instanceof final byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(value);
        return JSON.readTree(text);
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
        return new SaveUserMemoriesCommand(userId, conversationId, List.of(extractions));
    }

    private UserMemoryExtraction extraction(final UserMemoryType type, final String keyword, final List<String> categories) {
        return new UserMemoryExtraction(type, keyword + "을 좋아함", 2, categories, keyword, List.of(0.054f, -0.25f), 2);
    }

    private List<String> keywords(final String conversationId, final String deletedCondition) {
        return jdbc.queryForList("SELECT keyword FROM user_memories WHERE user_id = ? AND conversation_id = ? AND " + deletedCondition,
            String.class, userId, conversationId);
    }
}
