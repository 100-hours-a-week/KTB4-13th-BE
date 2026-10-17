package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

import com.book.core.memory.application.command.SaveUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemoryType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SaveUserMemoriesUseCaseIntegrationTest {
    private static final long USER_ID = 9001L;
    private static final String CONVERSATION_ID = "cv_20261013_a1";

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    SaveUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    UserMemoryRepositoryPort userMemoryRepository;

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
    void 빈_추출_결과면_같은_대화의_활성_기억만_지운다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));

        useCase.execute(command(CONVERSATION_ID));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_memories", Integer.class)).isZero();
    }

    @Test
    void 새_기억_저장에_실패하면_기존_활성_기억_삭제도_되돌린다() {
        useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.TOPIC, "추리 소설", List.of())));
        doThrow(new DataAccessResourceFailureException("save failed")).when(userMemoryRepository).saveAll(anyList());

        assertThatThrownBy(() -> useCase.execute(command(CONVERSATION_ID, extraction(UserMemoryType.MOOD, "잔잔한", List.of()))))
            .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(keywords(CONVERSATION_ID, "deleted_at IS NULL")).containsExactly("추리 소설");
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
