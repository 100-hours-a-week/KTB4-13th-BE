package com.book.core.memory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.memory.domain.UserMemoryType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class GetUserMemoriesUseCaseIntegrationTest {
    private static final long USER_ID = 9001L;
    private static final long OTHER_USER_ID = 9002L;

    @Autowired
    GetUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM user_memories");
        jdbc.update("DELETE FROM users");
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    @Test
    void 본인_활성_기억을_종류와_정규화한_핵심어로_묶어_최신순으로_반환한다() {
        insertMemory(101L, USER_ID, "TOPIC", "경제 책도 읽어요", 1, "[\"경제경영\"]", "경제", "2026-10-12 10:00:00", null);
        insertMemory(102L, USER_ID, "TOPIC", "경제 책은 별로예요", -2, "[\"경제경영\", \"자기계발\"]", " 경제 ", "2026-10-16 09:00:00", null);
        insertMemory(103L, USER_ID, "TOPIC", "경제 책을 좋아해요", 2, "[\"경제경영\"]", "경제", "2026-10-16 09:00:00", null);
        insertMemory(104L, USER_ID, "AUTHOR", "경제 작가를 좋아해요", 2, "[]", "경제", "2026-10-14 09:00:00", null);
        insertMemory(105L, USER_ID, "TOPIC", "지운 기억", 2, "[]", "경제", "2026-10-17 09:00:00", "2026-10-17 10:00:00");
        insertMemory(106L, OTHER_USER_ID, "TOPIC", "다른 사용자 기억", 2, "[]", "경제", "2026-10-18 09:00:00", null);

        final List<UserMemoryGroupResult> groups = useCase.execute(USER_ID);

        assertThat(groups).containsExactly(
            new UserMemoryGroupResult(List.of(103L, 102L, 101L), UserMemoryType.TOPIC, "경제 책을 좋아해요", List.of("경제경영", "자기계발"),
                List.of(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 12))),
            new UserMemoryGroupResult(List.of(104L), UserMemoryType.AUTHOR, "경제 작가를 좋아해요", List.of(), List.of(LocalDate.of(2026, 10, 14))));
    }

    @Test
    void 활성_기억이_없으면_빈_목록을_반환한다() {
        insertMemory(101L, USER_ID, "TOPIC", "지운 기억", 2, "[]", "경제", "2026-10-12 10:00:00", "2026-10-13 10:00:00");

        assertThat(useCase.execute(USER_ID)).isEmpty();
    }

    private void insertUser(final long id) {
        jdbc.update("""
            INSERT INTO users (id, nickname, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, id, "북적이" + id);
    }

    private void insertMemory(final long id, final long userId, final String type, final String value, final int strength,
        final String categories, final String keyword, final String createdAt, final String deletedAt) {
        jdbc.update("""
            INSERT INTO user_memories (
                id, user_id, type, value, strength, categories, keyword, vector, dim, conversation_id, created_at, updated_at, deleted_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, '[0.1]', 1, 'cv_1', ?, ?, ?)
            """, id, userId, type, value, strength, categories, keyword, createdAt, createdAt, deletedAt);
    }
}
