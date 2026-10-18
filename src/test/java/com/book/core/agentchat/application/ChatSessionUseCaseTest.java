package com.book.core.agentchat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.usecase.BeginChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CompleteChatRequestUseCase;
import com.book.core.agentchat.application.usecase.CreateChatSessionUseCase;
import com.book.core.agentchat.application.usecase.FailChatRequestUseCase;
import com.book.core.agentchat.application.usecase.GetChatHistoryUseCase;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import com.book.core.agentchat.infrastructure.persistence.mapper.JacksonChatResponseCodec;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.usecase.SaveRecommendationCardsUseCase;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class ChatSessionUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
    private static final Duration LEASE = Duration.ofSeconds(60);
    private final ChatRepositoryPort chats = mock(ChatRepositoryPort.class);
    private final Clock clock = mock(Clock.class);
    private final SaveRecommendationCardsUseCase saveCards = mock(SaveRecommendationCardsUseCase.class);
    private final JacksonChatResponseCodec codec = new JacksonChatResponseCodec(JsonMapper.builder().build());
    private final BeginChatRequestUseCase begin = new BeginChatRequestUseCase(chats, codec, clock);
    private final CompleteChatRequestUseCase complete = new CompleteChatRequestUseCase(chats, codec, saveCards, clock);
    private final FailChatRequestUseCase fail = new FailChatRequestUseCase(chats);
    private final GetChatHistoryUseCase history = new GetChatHistoryUseCase(chats, codec);
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, ChatRequest> requests = new HashMap<>();
    private ChatSession session;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        session = new ChatSession(7L);
        ReflectionTestUtils.setField(session, "id", 1L);
        when(chats.saveSession(any())).thenReturn(session);
        when(chats.findSession(1L)).thenReturn(Optional.of(session));
        when(chats.findSession(1L)).thenReturn(Optional.of(session));
        when(chats.findRequestForChange(any(), any()))
            .thenAnswer((final InvocationOnMock invocation) -> Optional.ofNullable(requests.get(invocation.getArgument(1))));
        when(chats.saveRequest(any())).thenAnswer((final InvocationOnMock invocation) -> {
            final ChatRequest request = invocation.getArgument(0);
            ReflectionTestUtils.setField(request, "id", (long) requests.size() + 1);
            requests.put(request.requestKey(), request);
            when(chats.getRequestForChange(request.id())).thenReturn(request);
            return request;
        });
        when(chats.recentMessages(1L, 20)).thenAnswer((final InvocationOnMock invocation) -> messages.stream()
            .sorted(Comparator.comparingLong(ChatMessage::sequence).reversed()).limit(20).toList().reversed());
        when(chats.messagesAfter(any(), any(Long.class), any(Integer.class))).thenAnswer((final InvocationOnMock invocation) -> messages
            .stream().filter((final ChatMessage message) -> message.sequence() > (long) invocation.getArgument(1))
            .limit((int) invocation.getArgument(2)).toList());
        when(chats.responseFor(any())).thenAnswer((final InvocationOnMock invocation) -> messages.stream()
            .filter(
                (final ChatMessage message) -> message.requestId().equals(invocation.getArgument(0)) && message.role().equals("assistant"))
            .findFirst());
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            messages.addAll(invocation.getArgument(0));
            return null;
        }).when(chats).saveMessages(anyList());
        when(saveCards.execute(any(), anyList())).thenReturn(List.of());
    }

    @Test
    void 대화방을_생성하고_성공한_발화와_응답만_다시_열어_복원한다() {
        assertThat(new CreateChatSessionUseCase(chats).execute(7L)).isEqualTo(1L);
        final SendChatMessageCommand command = command("one", "첫 질문");
        final ChatAttemptResult attempt = begin.execute(command, LEASE);
        assertThat(attempt.aiCommand().recentTurns()).isEmpty();
        assertThat(attempt.aiCommand().spec()).containsKeys("intent", "exact", "filters", "semantic", "anchor_book", "exclude");
        final ChatResponseSnapshot saved = complete.execute(command, attempt.generation(), outcome("답변", "추가 질문"));
        final ChatAttemptResult replay = begin.execute(command, LEASE);
        assertThat(replay.completedResponse()).isEqualTo(saved);
        assertThat(complete.execute(command, 99, outcome("다른 답변", null))).isEqualTo(saved);
        final List<ChatHistoryMessage> restored = history.execute(7L, 1L, 0, 100);
        assertThat(restored).hasSize(2);
        assertThat(restored.get(0).text()).isEqualTo("첫 질문");
        assertThat(restored.get(1).response().reply()).isEqualTo("답변");
        assertThat(restored.get(1).text()).isEqualTo("답변\n추가 질문");
        assertThat(history.execute(7L, 1L, 1, 1)).hasSize(1);
    }

    @Test
    void AI_실패_발화는_기록과_다음_맥락에서_제외하고_같은_키에는_오류를_재생한다() {
        final SendChatMessageCommand command = command("failed", "남기지 않을 질문");
        final ChatAttemptResult attempt = begin.execute(command, LEASE);
        fail.execute(command, attempt.generation(), ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        fail.execute(command, attempt.generation(), ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        assertThat(history.execute(7L, 1L, 0, 100)).isEmpty();
        assertThat(begin.execute(command, LEASE).failure()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        final ChatAttemptResult next = begin.execute(command("next", "다시 질문"), LEASE);
        assertThat(next.aiCommand().recentTurns()).isEmpty();
    }

    @Test
    void 최근_20개만_보내고_전체_기록과_긴_답변은_그대로_보관한다() {
        for (int index = 0; index < 12; index++) {
            final SendChatMessageCommand command = command("key" + index, "질문" + index);
            final ChatAttemptResult attempt = begin.execute(command, LEASE);
            complete.execute(command, attempt.generation(), outcome("가".repeat(230) + index, null));
        }
        final ChatAttemptResult next = begin.execute(command("last", "마지막"), LEASE);
        assertThat(next.aiCommand().recentTurns()).hasSize(20);
        assertThat(next.aiCommand().recentTurns().getFirst().text()).isEqualTo("질문2");
        assertThat(next.aiCommand().recentTurns().getLast().text()).hasSize(200);
        assertThat(history.execute(7L, 1L, 0, 100)).hasSize(24);
        assertThat(history.execute(7L, 1L, 0, 100).get(1).response().reply()).hasSize(231);
        assertThat(next.aiCommand().spec()).containsEntry("intent", "updated");
    }

    @Test
    void 만료_전_동시_전송과_같은_키의_다른_입력을_거절한다() {
        begin.execute(command("key", "질문"), LEASE);
        assertThatThrownBy(() -> begin.execute(command("key", "질문"), LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> begin.execute(command("other", "질문"), LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> begin.execute(command("key", "다른 질문"), LEASE)).isInstanceOf(CoreException.class);
        assertThat(messages).isEmpty();
    }

    @Test
    void 만료된_같은_요청은_새_세대로_재개하고_옛_응답과_실패는_막는다() {
        final SendChatMessageCommand command = command("key", "질문");
        final ChatAttemptResult first = begin.execute(command, LEASE);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        final ChatAttemptResult second = begin.execute(command, LEASE);
        assertThat(second.generation()).isEqualTo(2);
        assertThatThrownBy(() -> complete.execute(command, first.generation(), outcome("옛 답", null))).isInstanceOf(CoreException.class);
        fail.execute(command, first.generation(), ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        complete.execute(command, second.generation(), outcome("새 답", null));
        assertThat(history.execute(7L, 1L, 0, 100).getLast().response().reply()).isEqualTo("새 답");
    }

    @Test
    void 중단된_처리의_기한이_끝나면_새_발화가_방을_재사용한다() {
        final SendChatMessageCommand old = command("old", "옛 질문");
        final ChatAttemptResult attempt = begin.execute(old, LEASE);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        final SendChatMessageCommand next = command("new", "새 질문");
        final ChatAttemptResult active = begin.execute(next, LEASE);
        fail.execute(old, attempt.generation(), ErrorCode.AI_RECOMMENDATION_FAILURE);
        assertThat(begin.execute(old, LEASE).failure()).isEqualTo(ErrorCode.CHAT_ATTEMPT_EXPIRED);
        complete.execute(next, active.generation(), outcome("응답", null));
        assertThat(messages).hasSize(2);
    }

    @Test
    void 타인의_대화방과_없는_대화방_및_잘못된_조회_범위를_거절한다() {
        assertThatThrownBy(() -> history.execute(8L, 1L, 0, 10)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> history.execute(7L, 2L, 0, 10)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> history.execute(7L, 1L, -1, 10)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> history.execute(7L, 1L, 0, 101)).isInstanceOf(CoreException.class);
        final SendChatMessageCommand other = new SendChatMessageCommand(8L, 1L, "key", "질문", true, List.of());
        assertThatThrownBy(() -> begin.execute(other, LEASE)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> complete.execute(other, 1, outcome("응답", null))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> fail.execute(other, 1, ErrorCode.AI_RECOMMENDATION_FAILURE)).isInstanceOf(CoreException.class);
    }

    @Test
    void 기한을_넘긴_실패도_발화를_남기지_않고_방을_해제한다() {
        final SendChatMessageCommand command = command("key", "질문");
        final ChatAttemptResult attempt = begin.execute(command, LEASE);
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        fail.execute(command, attempt.generation(), ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        assertThat(session.activeRequestId()).isNull();
        assertThat(messages).isEmpty();
        assertThat(begin.execute(command, LEASE).failure()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
    }

    private SendChatMessageCommand command(final String key, final String text) {
        return new SendChatMessageCommand(7L, 1L, key, text, true, List.of());
    }

    private ChatRecommendationAiOutcome outcome(final String reply, final String followup) {
        return new ChatRecommendationAiOutcome(Map.of("intent", "updated"), reply, List.of(), followup, List.of("다음"), false);
    }
}
