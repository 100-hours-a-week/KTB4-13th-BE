package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class DeleteUserMemoriesUseCasesIntegrationTest {
    private static final long USER_ID = 9001L;
    private static final long OTHER_USER_ID = 9002L;
    private static final String DELETED_AT = "2026-10-01 10:00:00";

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    DeleteUserMemoriesUseCase deleteUserMemoriesUseCase;

    @Autowired
    DeleteAllUserMemoriesUseCase deleteAllUserMemoriesUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM user_memories");
        jdbc.update("DELETE FROM user_onboardings");
        jdbc.update("DELETE FROM users");
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
        insertMemory(101L, USER_ID, null);
        insertMemory(102L, USER_ID, null);
        insertMemory(103L, USER_ID, null);
        insertMemory(105L, USER_ID, DELETED_AT);
        insertMemory(201L, OTHER_USER_ID, null);
    }

    @Test
    void 선택한_본인_활성_기억만_중복_없이_Soft_Delete한다() {
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(USER_ID, List.of(101L)))).isEqualTo(1);
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(USER_ID, List.of(102L, 103L, 102L)))).isEqualTo(2);

        assertThat(activeIds()).containsExactly(201L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_memories", Integer.class)).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(longs = {201L, 999L})
    void 다른_회원의_기억이나_없는_기억이_있으면_아무것도_삭제하지_않고_404를_던진다(final long invalidId) {
        assertThatThrownBy(() -> deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(USER_ID, List.of(101L, invalidId))))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.USER_MEMORY_NOT_FOUND));

        assertThat(activeIds()).containsExactly(101L, 102L, 103L, 201L);
    }

    @Test
    void 이미_삭제한_본인_기억은_삭제_시각을_유지하고_성공한다() {
        assertThat(deleteUserMemoriesUseCase.execute(new DeleteUserMemoriesCommand(USER_ID, List.of(105L)))).isZero();

        assertThat(deletedAt(105L)).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    void 전체_삭제는_본인_활성_기억만_지우고_다른_회원_기억과_온보딩과_기존_삭제_기록을_유지한다() {
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status, completed_at) VALUES (?, 'COMPLETED', CURRENT_TIMESTAMP(6))",
            USER_ID);

        assertThat(deleteAllUserMemoriesUseCase.execute(USER_ID)).isEqualTo(3);

        assertThat(activeIds()).containsExactly(201L);
        assertThat(deletedAt(105L)).isEqualTo(LocalDateTime.of(2026, 10, 1, 10, 0));
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM user_onboardings WHERE user_id = ? AND deleted_at IS NULL", Integer.class, USER_ID))
            .isEqualTo(1);
    }

    @Test
    void 전체_삭제할_활성_기억이_없어도_성공한다() {
        deleteAllUserMemoriesUseCase.execute(USER_ID);

        assertThat(deleteAllUserMemoriesUseCase.execute(USER_ID)).isZero();
    }

    private List<Long> activeIds() {
        return jdbc.queryForList("SELECT id FROM user_memories WHERE deleted_at IS NULL ORDER BY id", Long.class);
    }

    private LocalDateTime deletedAt(final long id) {
        return jdbc.queryForObject("SELECT deleted_at FROM user_memories WHERE id = ?", LocalDateTime.class, id);
    }

    private void insertUser(final long id) {
        jdbc.update("""
            INSERT INTO users (id, nickname, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, "북적이" + id);
    }

    private void insertMemory(final long id, final long userId, final String deletedAt) {
        jdbc.update("""
            INSERT INTO user_memories (id, user_id, type, value, strength, categories, keyword, vector, dim, conversation_id, deleted_at)
            VALUES (?, ?, 'TOPIC', '경제 책을 좋아해요', 2, '[]', '경제', '[0.1]', 1, 'cv_1', ?)
            """, id, userId, deletedAt);
    }
}
