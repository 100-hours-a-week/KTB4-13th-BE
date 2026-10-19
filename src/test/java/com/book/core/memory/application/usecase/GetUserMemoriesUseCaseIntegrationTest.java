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

    @Autowired
    GetUserMemoriesUseCase useCase;

    @Autowired
    JdbcTemplate jdbc;

    Long userId;

    Long otherUserId;

    @BeforeEach
    void setUp() {
        userId = insertUser("기억조회회원");
        otherUserId = insertUser("기억조회타인");
    }

    @Test
    void 본인_활성_기억을_종류와_정규화한_핵심어로_묶어_최신순으로_반환한다() {
        final Long memory101 = insertMemory(userId, "TOPIC", "경제 책도 읽어요", 1, "[\"경제경영\"]", "경제", "2026-10-12 10:00:00", null);
        final Long memory102 = insertMemory(userId, "TOPIC", "경제 책은 별로예요", -2, "[\"경제경영\", \"자기계발\"]", " 경제 ", "2026-10-16 09:00:00", null);
        final Long memory103 = insertMemory(userId, "TOPIC", "경제 책을 좋아해요", 2, "[\"경제경영\"]", "경제", "2026-10-16 09:00:00", null);
        final Long memory104 = insertMemory(userId, "AUTHOR", "경제 작가를 좋아해요", 2, "[]", "경제", "2026-10-14 09:00:00", null);
        insertMemory(userId, "TOPIC", "지운 기억", 2, "[]", "경제", "2026-10-17 09:00:00", "2026-10-17 10:00:00");
        insertMemory(otherUserId, "TOPIC", "다른 사용자 기억", 2, "[]", "경제", "2026-10-18 09:00:00", null);

        final List<UserMemoryGroupResult> groups = useCase.execute(userId);

        assertThat(groups).containsExactly(
            new UserMemoryGroupResult(List.of(memory103, memory102, memory101), UserMemoryType.TOPIC, "경제 책을 좋아해요", List.of("경제경영", "자기계발"),
                List.of(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 12))),
            new UserMemoryGroupResult(List.of(memory104), UserMemoryType.AUTHOR, "경제 작가를 좋아해요", List.of(),
                List.of(LocalDate.of(2026, 10, 14))));
    }

    @Test
    void 활성_기억이_없으면_빈_목록을_반환한다() {
        insertMemory(userId, "TOPIC", "지운 기억", 2, "[]", "경제", "2026-10-12 10:00:00", "2026-10-13 10:00:00");

        assertThat(useCase.execute(userId)).isEmpty();
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertMemory(final Long userId, final String type, final String value, final int strength, final String categories,
        final String keyword, final String createdAt, final String deletedAt) {
        jdbc.update("""
            INSERT INTO user_memories (
                user_id, type, value, strength, categories, keyword, vector, dim, conversation_id, created_at, updated_at, deleted_at
            ) VALUES (?, ?, ?, ?, ? FORMAT JSON, ?, '[0.1]' FORMAT JSON, 1, 'cv_1', ?, ?, ?)
            """, userId, type, value, strength, categories, keyword, createdAt, createdAt, deletedAt);
        return jdbc.queryForObject("SELECT MAX(id) FROM user_memories WHERE user_id = ?", Long.class, userId);
    }
}
