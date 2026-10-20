package com.book.core.memory.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.memory.application.port.UserMemoryRepositoryPort;
import com.book.core.memory.domain.UserMemory;
import com.book.core.memory.domain.UserMemoryType;
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
class UserMemoryRepositoryIntegrationTest {
    private static final String NICKNAME = "기억저장소회원";

    @Autowired
    UserMemoryRepositoryPort userMemoryRepository;

    @Autowired
    JdbcTemplate jdbc;

    Long userId;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", NICKNAME);
        userId = jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, NICKNAME);
    }

    @Test
    void 카테고리와_벡터를_JSON_배열로_저장하고_목록으로_복원한다() {
        userMemoryRepository.saveAll(List.of(new UserMemory(userId, UserMemoryType.TOPIC, "추리 소설을 좋아함", 2, List.of("추리/스릴러", "소설"), "추리 소설",
            List.of(0.054f, -0.0028f, 1.0f), 3, "cv_1")));
        jdbc.update("UPDATE user_memories SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ?", userId);

        final UserMemory found = userMemoryRepository.findDeletedByUserId(userId).getFirst();

        assertThat(found.categories()).containsExactly("추리/스릴러", "소설");
        assertThat(found.vector()).containsExactly(0.054f, -0.0028f, 1.0f);
        assertThat(found.createdAt()).isNotNull();
    }

}
