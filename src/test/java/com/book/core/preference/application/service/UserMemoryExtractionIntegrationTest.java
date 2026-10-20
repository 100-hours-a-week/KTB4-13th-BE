package com.book.core.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.application.service.ChatSessionService;
import com.book.core.agentchat.application.usecase.BeginChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CompleteChatRequestUseCase;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionClient;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import com.book.core.memory.domain.UserMemoryType;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class UserMemoryExtractionIntegrationTest {
    private static final AtomicLong NEXT_ID = new AtomicLong(3100000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private UserPreferenceService userPreferenceService;
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private BeginChatRequestUseCase begin;
    @Autowired
    private CompleteChatRequestUseCase complete;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private AiMemoryExtractionClient extractionClient;
    @MockitoBean
    private AiRecommendationClient recommendationClient;
    @MockitoBean
    private Clock clock;
    private long userId;

    @BeforeEach
    void setUp() {
        nightAfterToday();
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recommendationClient.chat(any()))
            .thenReturn(new AiRecommendationChatResult(Map.of("intent", "semantic"), "답변", List.of(), null, List.of(), false));
        userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "memory" + userId);
    }

    @Test
    void 야간_배치는_세션_전체_대화를_트랜잭션_밖에서_추출하고_다시_실행해도_같은_순번은_추출하지_않는다() {
        agree();
        final Long sessionId = chatSessionService.create(userId);
        send(sessionId, "first", "추리 소설 좋아해요");
        send(sessionId, "second", "김영하 작가도요");
        when(extractionClient.extract(argThat(this::mine))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(extraction("추리 소설"));
        });

        userPreferenceService.extractNightlyMemories();
        userPreferenceService.extractNightlyMemories();

        verify(extractionClient, times(1)).extract(argThat((final AiMemoryExtractionRequest request) -> mine(request) && request.consented()
            && request.conversationId().equals(String.valueOf(sessionId))
            && request.conversation()
                .equals(List.of(new ConversationMessage("user", "추리 소설 좋아해요"), new ConversationMessage("assistant", "답변"),
                    new ConversationMessage("user", "김영하 작가도요"), new ConversationMessage("assistant", "답변")))));
        assertThat(activeKeywords(sessionId)).containsExactly("추리 소설");
        assertThat(extractedSequence(sessionId)).isEqualTo(4);
    }

    @Test
    void 같은_회원의_세션은_마지막_메시지가_이른_순으로_처리하고_실패하면_뒤_세션을_다음_밤으로_미룬다() {
        agree();
        final Long later = chatSessionService.create(userId);
        final Long earlier = chatSessionService.create(userId);
        send(earlier, "earlier", "먼저 말한 대화");
        send(later, "later", "나중에 말한 대화");
        when(extractionClient.extract(argThat(this::mine))).thenThrow(new CoreException(ErrorCode.AI_MEMORY_EXTRACTION_UNAVAILABLE));

        userPreferenceService.extractNightlyMemories();

        verify(extractionClient, never()).extract(
            argThat((final AiMemoryExtractionRequest request) -> mine(request) && request.conversationId().equals(String.valueOf(later))));
        assertThat(extractedSequence(earlier)).isZero();
        assertThat(extractedSequence(later)).isZero();

        when(extractionClient.extract(argThat(this::mine))).thenReturn(List.of(extraction("잔잔한")));
        userPreferenceService.extractNightlyMemories();

        final InOrder order = inOrder(extractionClient);
        order.verify(extractionClient, times(2)).extract(argThat(
            (final AiMemoryExtractionRequest request) -> mine(request) && request.conversationId().equals(String.valueOf(earlier))));
        order.verify(extractionClient).extract(
            argThat((final AiMemoryExtractionRequest request) -> mine(request) && request.conversationId().equals(String.valueOf(later))));
        assertThat(extractedSequence(earlier)).isEqualTo(2);
        assertThat(extractedSequence(later)).isEqualTo(2);
    }

    @Test
    void 요청이_많다는_응답이면_Retry_After만큼_기다린_뒤_다시_부른다() {
        agree();
        final Long sessionId = chatSessionService.create(userId);
        send(sessionId, "first", "로맨스는 별로예요");
        when(extractionClient.extract(argThat(this::mine)))
            .thenThrow(new RetryAfterException(ErrorCode.AI_MEMORY_EXTRACTION_RATE_LIMITED, 0)).thenReturn(List.of(extraction("로맨스 소설")));

        userPreferenceService.extractNightlyMemories();

        verify(extractionClient, times(2)).extract(argThat(this::mine));
        assertThat(activeKeywords(sessionId)).containsExactly("로맨스 소설");
        assertThat(extractedSequence(sessionId)).isEqualTo(2);
    }

    @Test
    void 개인화_추천에_동의하지_않았으면_AI를_부르지_않고_완료_순번도_바꾸지_않는다() {
        final Long sessionId = chatSessionService.create(userId);
        send(sessionId, "first", "추리 소설 좋아해요");

        userPreferenceService.extractNightlyMemories();

        verify(extractionClient, never()).extract(argThat(this::mine));
        assertThat(extractedSequence(sessionId)).isZero();
    }

    @Test
    void 오늘_0시_이후의_대화는_다음_밤에_처리한다() {
        agree();
        final Long sessionId = chatSessionService.create(userId);
        send(sessionId, "first", "추리 소설 좋아해요");
        when(clock.instant()).thenReturn(Instant.now());

        userPreferenceService.extractNightlyMemories();

        verify(extractionClient, never()).extract(argThat(this::mine));
        assertThat(extractedSequence(sessionId)).isZero();
    }

    @Test
    void 배치가_완료_순번을_갱신해도_진행_중인_대화는_완료되고_완료_순번을_덮어쓰지_않는다() {
        agree();
        final Long sessionId = chatSessionService.create(userId);
        send(sessionId, "first", "추리 소설 좋아해요");
        final SendChatMessageCommand next = new SendChatMessageCommand(userId, sessionId, "second", "김영하 작가도요", true, List.of());
        final ChatAttemptResult attempt = begin.execute(next, Duration.ofSeconds(60));

        userPreferenceService.extractNightlyMemories();
        complete.execute(next, attempt.generation(),
            new ChatRecommendationAiOutcome(Map.of("intent", "semantic"), "답변", List.of(), null, List.of(), false));

        assertThat(extractedSequence(sessionId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id = ?", Integer.class, sessionId)).isEqualTo(4);
    }

    // 메시지 created_at은 실제 시각으로 저장되므로 서비스 시각을 다음 날로 옮겨 오늘 대화를 지난 대화로 만듭니다.
    private void nightAfterToday() {
        when(clock.instant()).thenReturn(Instant.now().plus(Duration.ofDays(1)));
    }

    private void send(final Long sessionId, final String key, final String message) {
        chatSessionService.send(new SendChatMessageCommand(userId, sessionId, key, message, true, List.of()));
    }

    private boolean mine(final AiMemoryExtractionRequest request) {
        return request != null && request.userId().equals(userId);
    }

    private UserMemoryExtraction extraction(final String keyword) {
        return new UserMemoryExtraction(UserMemoryType.TOPIC, keyword + "을 좋아함", 2, List.of(), keyword, List.of(0.2f), 1);
    }

    private void agree() {
        jdbc.update("""
            INSERT INTO user_term_agreement (user_id, term_id, action, agreed_at)
            SELECT ?, id, 'AGREE', CURRENT_TIMESTAMP(6) FROM terms WHERE term_type = 'PERSONALIZED_RECOMMENDATION'
            """, userId);
    }

    private List<String> activeKeywords(final Long sessionId) {
        return jdbc.queryForList("SELECT keyword FROM user_memories WHERE user_id = ? AND conversation_id = ? AND deleted_at IS NULL",
            String.class, userId, String.valueOf(sessionId));
    }

    private long extractedSequence(final Long sessionId) {
        return jdbc.queryForObject("SELECT memory_extracted_sequence FROM chat_sessions WHERE id = ?", Long.class, sessionId);
    }
}
