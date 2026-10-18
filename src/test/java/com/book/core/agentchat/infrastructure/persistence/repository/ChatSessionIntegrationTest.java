package com.book.core.agentchat.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.service.ChatSessionService;
import com.book.core.agentchat.application.usecase.BeginChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CompleteChatRequestUseCase;
import com.book.core.agentchat.infrastructure.persistence.mapper.JacksonChatResponseCodec;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ChatSessionIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
    private static final Duration LEASE = Duration.ofSeconds(60);
    private static final AtomicLong NEXT_ID = new AtomicLong(1900000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private ChatSessionService service;
    @Autowired
    private BeginChatRequestUseCase begin;
    @Autowired
    private CompleteChatRequestUseCase complete;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private Clock clock;
    @MockitoBean
    private AiRecommendationClient ai;
    @MockitoBean
    private ChatResponseCodec codec;
    private final ChatResponseCodec realCodec = new JacksonChatResponseCodec(JsonMapper.builder().build());
    private long userId;
    private Long sessionId;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(codec.encode(any())).thenAnswer((final InvocationOnMock invocation) -> realCodec.encode(invocation.getArgument(0)));
        when(codec.decode(any())).thenAnswer((final InvocationOnMock invocation) -> realCodec.decode(invocation.getArgument(0)));
        when(ai.chat(any())).thenAnswer((final InvocationOnMock invocation) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return response("답변");
        });
        userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "chat" + userId);
        sessionId = service.create(userId);
    }

    @Test
    void 응답_유실_후_재전송은_AI_재호출_없이_같은_완성_응답을_반환한다() {
        final SendChatMessageCommand command = command("one", "질문");
        final ChatResponseSnapshot saved = service.send(command);
        final ChatResponseSnapshot replay = service.send(command);
        assertThat(replay).isEqualTo(saved);
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2)
            .allSatisfy((final ChatHistoryMessage message) -> assertThat(message.createdAt()).isNotNull());
        verify(ai, times(1)).chat(any());
    }

    @Test
    void 실패_발화는_보관하지_않고_다음_질문에_섞지_않는다() {
        when(ai.chat(any())).thenThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT));
        final SendChatMessageCommand failed = command("failed", "실패 질문");
        assertThatThrownBy(() -> service.send(failed)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.send(failed)).isInstanceOf(CoreException.class);
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).isEmpty();
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            assertThat(request.recentTurns()).isEmpty();
            return response("새 답변");
        }).when(ai).chat(any());
        service.send(command("new", "다음 질문"));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
        verify(ai, times(2)).chat(any());
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"AI_RECOMMENDATION_RATE_LIMITED", "AI_RECOMMENDATION_UNAVAILABLE"})
    void 재시도_간격_오류도_방을_해제하고_같은_키에_간격까지_재생한다(final ErrorCode error) {
        Mockito.doThrow(new RetryAfterException(error, 17)).when(ai).chat(any());
        final SendChatMessageCommand command = command("retry-after", "질문");
        assertThatThrownBy(() -> service.send(command)).isInstanceOfSatisfying(RetryAfterException.class,
            (final RetryAfterException exception) -> assertThat(exception.retryAfterSeconds()).isEqualTo(17));
        assertThatThrownBy(() -> service.send(command)).isInstanceOfSatisfying(RetryAfterException.class,
            (final RetryAfterException exception) -> {
                assertThat(exception.errorCode()).isEqualTo(error);
                assertThat(exception.retryAfterSeconds()).isEqualTo(17);
            });
        verify(ai, times(1)).chat(any());
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).isEmpty();
        Mockito.doReturn(response("새 답변")).when(ai).chat(any());
        service.send(command("new", "다음 질문"));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
    }

    @Test
    void 상품과_책이_삭제돼도_과거_카드는_복원하고_새_추천에서는_제외한다() {
        createProduct();
        when(ai.chat(any())).thenReturn(new AiRecommendationChatResult(Map.of("intent", "semantic"), "추천합니다",
            List.of(new AiRecommendationCard(userId, 93, "짧은 이유", "긴 이유")), "어떠세요?", List.of("다른 책"), true));
        final ChatResponseSnapshot sent = service.send(command("card", "추천해 줘"));
        jdbc.update("UPDATE products SET discounted_price = 15000 WHERE id = ?", userId);
        jdbc.update("UPDATE books SET title = '바뀐 제목', deleted_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
        jdbc.update("UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
        final ChatResponseSnapshot restored = service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100)).getLast().response();
        assertThat(restored).isEqualTo(sent);
        assertThat(restored.cards().getFirst().title()).isEqualTo("당시 제목");
        assertThat(restored.cards().getFirst().price()).isEqualByComparingTo("12000.00");
        assertThat(restored.cards().getFirst().recommendationCardId()).isNotNull();
        assertThat(restored.followup()).isEqualTo("어떠세요?");
        assertThat(restored.buttons()).containsExactly("다른 책");
        assertThat(restored.degraded()).isTrue();
        final ChatResponseSnapshot next = service.send(command("after-delete", "다시 추천해 줘"));
        assertThat(next.cards()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendation_cards WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100)).get(1).response()).isEqualTo(sent);
    }

    @Test
    void 전체_기록은_남기고_서버에서_해당_방의_최근_20개만_구성한다() {
        for (int index = 0; index < 11; index++) {
            service.send(command("key" + index, "질문" + index));
        }
        when(ai.chat(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            assertThat(request.recentTurns()).hasSize(20);
            assertThat(request.recentTurns().getFirst().text()).isEqualTo("질문1");
            assertThat(request.message()).isEqualTo("마지막 질문");
            return response("최종 응답");
        });
        service.send(command("last", "마지막 질문"));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(24);
    }

    @Test
    void 같은_방의_처리중_새_발화는_거절하고_다른_방은_독립_처리한다() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        when(ai.chat(any())).thenAnswer((final InvocationOnMock invocation) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return response("답변");
        });
        try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<ChatResponseSnapshot> first = executor.submit(() -> service.send(command("first", "처음")));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> service.send(command("second", "동시"))).isInstanceOfSatisfying(CoreException.class,
                    (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CHAT_IN_PROGRESS));
                final Long otherSession = service.create(userId);
                assertThat(begin.execute(new SendChatMessageCommand(userId, otherSession, "independent", "다른 방", true, List.of()), LEASE)
                    .aiCommand().recentTurns()).isEmpty();
            } finally {
                release.countDown();
            }
            first.get(10, TimeUnit.SECONDS);
        }
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
    }

    @Test
    void 만료_후_재개한_새_세대만_확정하고_옛_응답은_버린다() {
        final SendChatMessageCommand command = command("resume", "질문");
        final ChatAttemptResult first = begin.execute(command, LEASE);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        final ChatAttemptResult second = begin.execute(command, LEASE);
        assertThatThrownBy(() -> complete.execute(command, first.generation(), outcome("늦은 응답"))).isInstanceOf(CoreException.class);
        complete.execute(command, second.generation(), outcome("복구된 응답"));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100)).getLast().response().reply()).isEqualTo("복구된 응답");
    }

    @Test
    void 카드_저장_뒤_응답_직렬화가_실패해도_발화와_카드를_모두_롤백한다() {
        createProduct();
        when(ai.chat(any())).thenReturn(new AiRecommendationChatResult(Map.of(), "추천",
            List.of(new AiRecommendationCard(userId, 90, "이유", "긴 이유")), null, List.of(), false));
        when(codec.encode(any())).thenThrow(new IllegalStateException("test serialization failure"));
        assertThatThrownBy(() -> service.send(command("rollback", "질문"))).isInstanceOf(IllegalStateException.class);
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendation_cards WHERE user_id = ?", Integer.class, userId)).isZero();
        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        Mockito.doAnswer((final InvocationOnMock invocation) -> realCodec.encode(invocation.getArgument(0))).when(codec).encode(any());
        service.send(command("rollback", "질문"));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
    }

    @Test
    void 타인의_방은_조회와_전송을_거절하고_같은_키의_내용_변경도_거절한다() {
        assertThatThrownBy(() -> service.history(new GetChatHistoryCommand(userId + 1, sessionId, 0, 100)))
            .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> service.send(new SendChatMessageCommand(userId + 1, sessionId, "key", "질문", true, List.of())))
            .isInstanceOf(CoreException.class);
        service.send(command("same", "원래 질문"));
        assertThatThrownBy(() -> service.send(command("same", "바뀐 질문"))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        assertThat(service.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
    }

    private SendChatMessageCommand command(final String key, final String message) {
        return new SendChatMessageCommand(userId, sessionId, key, message, true, List.of());
    }

    private AiRecommendationChatResult response(final String reply) {
        return new AiRecommendationChatResult(Map.of("intent", "semantic"), reply, List.of(), null, List.of("다음"), false);
    }

    private ChatRecommendationAiOutcome outcome(final String reply) {
        return new ChatRecommendationAiOutcome(Map.of("intent", "semantic"), reply, List.of(), null, List.of(), false);
    }

    private void createProduct() {
        jdbc.update("""
            INSERT INTO books (id, title, author, publisher, category, published_at, created_at, updated_at)
            VALUES (?, '당시 제목', '작가', '출판사', '소설', '2026-01-01', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, userId);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity, created_at, updated_at)
            VALUES (?, ?, '상품', 15000, 12000, 9000, 10, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, userId, userId);
    }
}
