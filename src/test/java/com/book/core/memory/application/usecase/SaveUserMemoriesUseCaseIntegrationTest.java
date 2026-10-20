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
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    SaveUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    Long userId;

    Long sessionId;

    Long otherSessionId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", NICKNAME);
        userId = jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, NICKNAME);
        sessionId = insertSession();
        otherSessionId = insertSession();
    }

    @Test
    void 최초_추출_결과를_세션과_함께_저장하고_완료_순번을_갱신한다() {
        assertThat(useCase.execute(command(sessionId, 2, extraction(UserMemoryType.TOPIC, "추리 소설", List.of("추리/스릴러"))))).isTrue();

        final var row = jdbc.queryForMap("SELECT * FROM user_memories WHERE user_id = ?", userId);
        assertThat(row).containsEntry("user_id", userId).containsEntry("type", "TOPIC").containsEntry("keyword", "추리 소설")
            .containsEntry("strength", 2).containsEntry("dim", 2).containsEntry("conversation_id", String.valueOf(sessionId));
        assertThat(json(row.get("categories"))).isEqualTo(JSON.readTree("[\"추리/스릴러\"]"));
        assertThat(json(row.get("vector"))).isEqualTo(JSON.readTree("[0.054, -0.25]"));
        assertThat(row.get("deleted_at")).isNull();
        assertThat(extractedSequence(sessionId)).isEqualTo(2);
    }

    @Test
    void 같은_세션을_재추출하면_그_세션의_활성_기억만_교체하고_삭제한_취향은_모든_세션에서_다시_저장하지_않는다() {
        useCase.execute(command(sessionId, 2, extraction(UserMemoryType.TOPIC, "추리 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, "김영하", List.of())));
        useCase.execute(command(otherSessionId, 2, extraction(UserMemoryType.MOOD, "잔잔한", List.of())));
        jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ? AND keyword = '김영하'", userId);

        useCase.execute(command(otherSessionId, 4, extraction(UserMemoryType.MOOD, "잔잔한", List.of()),
            extraction(UserMemoryType.AUTHOR, " 김영하 ", List.of()), extraction(UserMemoryType.TOPIC, "김영하", List.of())));
        useCase.execute(command(sessionId, 4, extraction(UserMemoryType.TOPIC, "로맨스 소설", List.of()),
            extraction(UserMemoryType.AUTHOR, "김영하", List.of())));

        assertThat(keywords(otherSessionId, "deleted_at IS NULL")).containsExactlyInAnyOrder("잔잔한", "김영하");
        assertThat(jdbc.queryForList("SELECT type FROM user_memories WHERE user_id = ? AND deleted_at IS NULL AND keyword = '김영하'",
            String.class, userId)).containsExactly("TOPIC");
        assertThat(keywords(sessionId, "deleted_at IS NULL")).containsExactly("로맨스 소설");
        assertThat(keywords(sessionId, "deleted_at IS NOT NULL")).containsExactly("김영하");
    }

    @Test
    void 이미_반영한_순번이거나_다른_회원의_세션이면_기억과_완료_순번을_바꾸지_않는다() {
        useCase.execute(command(sessionId, 4, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));

        assertThat(useCase.execute(command(sessionId, 4, extraction(UserMemoryType.TOPIC, "로맨스 소설", List.of())))).isFalse();
        assertThat(useCase.execute(command(sessionId, 2))).isFalse();
        assertThat(useCase.execute(new SaveUserMemoriesCommand(userId + 1, sessionId, 6, List.of()))).isFalse();

        assertThat(keywords(sessionId, "deleted_at IS NULL")).containsExactly("추리 소설");
        assertThat(extractedSequence(sessionId)).isEqualTo(4);
    }

    @Test
    void 같은_회원의_삭제가_진행_중이면_재추출은_삭제_커밋을_기다린_뒤_삭제_기억을_다시_저장하지_않는다() throws Exception {
        useCase.execute(command(sessionId, 2, extraction(UserMemoryType.AUTHOR, "김영하", List.of())));
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

        final CompletableFuture<Void> reExtraction =
            CompletableFuture.runAsync(() -> useCase.execute(command(sessionId, 4, extraction(UserMemoryType.AUTHOR, "김영하", List.of()))));
        Thread.sleep(500);
        final boolean finishedBeforeDeleteCommit = reExtraction.isDone();
        commitDelete.countDown();
        deletion.get(10, TimeUnit.SECONDS);
        reExtraction.get(10, TimeUnit.SECONDS);

        assertThat(finishedBeforeDeleteCommit).isFalse();
        assertThat(keywords(sessionId, "deleted_at IS NULL")).isEmpty();
        assertThat(keywords(sessionId, "deleted_at IS NOT NULL")).containsExactly("김영하");
        assertThat(extractedSequence(sessionId)).isEqualTo(4);
    }

    @Test
    void 빈_추출_결과면_같은_세션의_활성_기억만_지우고_완료_순번을_갱신한다() {
        useCase.execute(command(sessionId, 2, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));
        useCase.execute(command(otherSessionId, 2, extraction(UserMemoryType.MOOD, "잔잔한", List.of())));

        useCase.execute(command(sessionId, 4));

        assertThat(keywords(sessionId, "deleted_at IS NULL")).isEmpty();
        assertThat(keywords(otherSessionId, "deleted_at IS NULL")).containsExactly("잔잔한");
        assertThat(extractedSequence(sessionId)).isEqualTo(4);
    }

    @Test
    void 잘못된_새_기억이면_기존_활성_기억과_완료_순번을_함께_유지한다() {
        useCase.execute(command(sessionId, 2, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));
        final var tooLongValue =
            new UserMemoryExtraction(UserMemoryType.TOPIC, "가".repeat(201), 2, List.of(), "로맨스 소설", List.of(0.054f, -0.25f), 2);

        assertThatThrownBy(() -> useCase.execute(command(sessionId, 4, extraction(UserMemoryType.MOOD, "잔잔한", List.of()), tooLongValue)))
            .isInstanceOf(CoreException.class).extracting(exception -> ((CoreException) exception).errorCode())
            .isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThat(keywords(sessionId, "deleted_at IS NULL")).containsExactly("추리 소설");
        assertThat(extractedSequence(sessionId)).isEqualTo(2);
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

    private SaveUserMemoriesCommand command(final Long sessionId, final long sequence, final UserMemoryExtraction... extractions) {
        return new SaveUserMemoriesCommand(userId, sessionId, sequence, List.of(extractions));
    }

    private Long insertSession() {
        jdbc.update("INSERT INTO chat_sessions (user_id, next_sequence, version, created_at, updated_at) "
            + "VALUES (?, 1, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", userId);
        return jdbc.queryForObject("SELECT MAX(id) FROM chat_sessions WHERE user_id = ?", Long.class, userId);
    }

    private long extractedSequence(final Long sessionId) {
        return jdbc.queryForObject("SELECT memory_extracted_sequence FROM chat_sessions WHERE id = ?", Long.class, sessionId);
    }

    private UserMemoryExtraction extraction(final UserMemoryType type, final String keyword, final List<String> categories) {
        return new UserMemoryExtraction(type, keyword + "을 좋아함", 2, categories, keyword, List.of(0.054f, -0.25f), 2);
    }

    private List<String> keywords(final Long sessionId, final String deletedCondition) {
        return jdbc.queryForList("SELECT keyword FROM user_memories WHERE user_id = ? AND conversation_id = ? AND " + deletedCondition,
            String.class, userId, String.valueOf(sessionId));
    }
}
