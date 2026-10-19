package com.book.core.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.ExtractUserMemoriesCommand;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionClient;
import com.book.core.memory.domain.UserMemoryType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class UserMemoryExtractionIntegrationTest {
    private static final String NICKNAME = "기억추출연동회원";
    private static final String CONVERSATION_ID = "cv_1";

    @Autowired
    UserPreferenceService userPreferenceService;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    AiMemoryExtractionClient aiMemoryExtractionClient;

    Long userId;

    ExtractUserMemoriesCommand command;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", NICKNAME);
        userId = jdbc.queryForObject("SELECT MAX(id) FROM users WHERE nickname = ?", Long.class, NICKNAME);
        command = new ExtractUserMemoriesCommand(userId, CONVERSATION_ID, List.of(new ConversationMessage("user", "로맨스 소설 좋아해요")));
        jdbc.update("""
            INSERT INTO user_memories (user_id, type, value, strength, categories, keyword, vector, dim, conversation_id)
            VALUES (?, 'TOPIC', '추리 소설을 좋아함', 2, '[]', '추리 소설', '[0.1]', 1, ?)
            """, userId, CONVERSATION_ID);
    }

    @Test
    void 동의한_사용자는_트랜잭션_밖에서_AI를_호출하고_추출_결과로_기억을_교체한다() {
        agree();
        when(aiMemoryExtractionClient.extract(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(new UserMemoryExtraction(UserMemoryType.TOPIC, "로맨스 소설을 좋아함", 2, List.of("로맨스"), "로맨스 소설", List.of(0.2f), 1));
        });

        userPreferenceService.extractMemories(command);

        assertThat(activeKeywords()).containsExactly("로맨스 소설");
    }

    @Test
    void 빈_추출_결과면_같은_대화의_활성_기억을_지운다() {
        agree();
        when(aiMemoryExtractionClient.extract(any())).thenReturn(List.of());

        userPreferenceService.extractMemories(command);

        assertThat(activeKeywords()).isEmpty();
    }

    @Test
    void 개인화_추천에_동의하지_않았으면_AI를_호출하지_않고_기억을_유지한다() {
        userPreferenceService.extractMemories(command);

        verifyNoInteractions(aiMemoryExtractionClient);
        assertThat(activeKeywords()).containsExactly("추리 소설");
    }

    @Test
    void AI_호출이나_응답이_잘못되면_기존_기억을_유지한다() {
        agree();
        when(aiMemoryExtractionClient.extract(any())).thenThrow(new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE));

        assertThatThrownBy(() -> userPreferenceService.extractMemories(command)).isInstanceOf(CoreException.class);

        assertThat(activeKeywords()).containsExactly("추리 소설");
    }

    @Test
    void AI가_저장_규칙에_맞지_않는_값을_주면_기존_기억을_유지한다() {
        agree();
        when(aiMemoryExtractionClient.extract(any()))
            .thenReturn(List.of(new UserMemoryExtraction(UserMemoryType.TOPIC, "로맨스 소설을 좋아함", 0, List.of(), "로맨스 소설", List.of(0.2f), 1)));

        assertThatThrownBy(() -> userPreferenceService.extractMemories(command)).isInstanceOf(CoreException.class);

        assertThat(activeKeywords()).containsExactly("추리 소설");
    }

    private void agree() {
        jdbc.update("""
            INSERT INTO user_term_agreement (user_id, term_id, action, agreed_at)
            SELECT ?, id, 'AGREE', CURRENT_TIMESTAMP(6) FROM terms WHERE term_type = 'PERSONALIZED_RECOMMENDATION'
            """, userId);
    }

    private List<String> activeKeywords() {
        return jdbc.queryForList("SELECT keyword FROM user_memories WHERE user_id = ? AND deleted_at IS NULL", String.class, userId);
    }
}
